package dev.privado.eliteredux;

import android.app.*;
import android.content.*;
import android.content.res.Configuration;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.io.*;
import java.nio.file.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.zip.*;
import java.util.concurrent.*;

public final class MainActivity extends Activity {
    private String BASE;
    private GameSpec spec;
    private Settings settings;
    private static final int EXPORT = 10, IMPORT_SAVE = 11, IMPORT_STATE = 12, EXPORT_SAVE = 13;
    private Engine engine;
    private GameLayout layout;
    private PlayerMenu playerMenu;
    private boolean inGame, sessionStarted, busy, checkpointQueued;
    private final ExecutorService files=Executors.newSingleThreadExecutor(r->new Thread(r,"Arquivos do jogo"));
    private File folder;
    private int selectedSlot = 5, importSlot;
    private boolean picker;
    private boolean activityResumed;
    private final Handler ui = new Handler(Looper.getMainLooper());
    private final Runnable telemetry = new Runnable() { public void run() {
        if (layout != null && engine != null) layout.updateStatus(String.format(Locale.US, "%.0f FPS  ·  %d×", engine.fps, engine.speed));
        ui.postDelayed(this, 1500);
    }};
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        settings=new Settings(this); applyOrientation(); applyAwake();
        immersive();
        TextView loading = new TextView(this); loading.setText("Preparando seu jogo e seus saves…"); loading.setTextColor(GameLayout.INK); loading.setBackgroundColor(GameLayout.BG); loading.setTextSize(20); loading.setGravity(Gravity.CENTER); setContentView(loading);
        selectedSlot = getPreferences(0).getInt("slot", 5);
        new Thread(() -> {
            try {
                spec=GameSpec.read(this); BASE=spec.base;
                File storage = getExternalFilesDir(null);
                if (storage == null) storage = getFilesDir();
                folder = new File(storage, spec.folder);
                if (!folder.isDirectory() && !folder.mkdirs()) throw new IOException("Não foi possível criar a pasta do jogo.");
                File originals = new File(folder, "originais");
                if (!originals.isDirectory() && !originals.mkdirs()) throw new IOException("Não foi possível criar a pasta dos saves originais.");
                for (String file : getAssets().list("game")) {
                    copyAssetOnce(file, new File(folder, file));
                    if (!file.endsWith(".gba")) copyAssetOnce(file, new File(originals, file));
                }
                Engine prepared = new Engine(folder, BASE, message -> runOnUiThread(() -> toast(message)));
                prepared.start(); settings.apply(prepared);
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) {prepared.stop();return;}
                    try {
                        engine = prepared;
                        settings.apply(engine);
                        layout = new GameLayout(this, engine, settings); setContentView(layout);
                        engine.resumed = false; ui.post(telemetry);
                        enterGame();
                    } catch (Throwable ex) { fatal(ex); }
                });
            } catch (Throwable ex) { runOnUiThread(() -> fatal(ex)); }
        }, "Preparar arquivos").start();
    }
    private void copyAssetOnce(String name, File target) throws IOException {
        if (target.isFile()) return;
        File temp = new File(target.getPath()+".tmp");
        try (InputStream in = getAssets().open("game/"+name); FileOutputStream out = new FileOutputStream(temp)) { copy(in,out); out.getFD().sync(); }
        Files.move(temp.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
    }
    private static void copy(InputStream in, OutputStream out) throws IOException { byte[] buf=new byte[32768]; int n; while((n=in.read(buf))!=-1) out.write(buf,0,n); }
    private void fatal(Throwable error) { show(new AlertDialog.Builder(this).setTitle("Não foi possível iniciar").setMessage(error.toString()).setPositiveButton("Fechar",(d,w)->finish())); }
    private void immersive() { getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY); }
    @Override public void onWindowFocusChanged(boolean focus) { super.onWindowFocusChanged(focus); if(focus) immersive(); }
    @Override public void onConfigurationChanged(Configuration c) { super.onConfigurationChanged(c); if(layout!=null){layout.clearKeys(); layout.requestLayout();} if(playerMenu!=null)ui.post(()->playerMenu.resize()); immersive(); }
    @Override protected void onResume() { super.onResume(); activityResumed=true; if(engine!=null) engine.resumed=activityResumed&&inGame&&!picker; }
    @Override protected void onPause() { activityResumed=false; if(engine!=null){engine.resumed=false; if(layout!=null)layout.clearKeys(); if(inGame&&sessionStarted)scheduleCheckpoint();} super.onPause(); }
    @Override protected void onDestroy() { ui.removeCallbacksAndMessages(null); if(engine!=null){Engine closing=engine;files.execute(closing::stop);}files.shutdown();super.onDestroy(); }
    @Override public void onBackPressed() { if(layout!=null&&layout.isEditing())finishHudEdit();else if(engine!=null&&inGame)menu(); else super.onBackPressed(); }
    AlertDialog show(AlertDialog.Builder b) {
        if(engine!=null) { engine.dialogs++; if(layout!=null)layout.clearKeys(); }
        AlertDialog d=b.create();
        d.setOnDismissListener(v->{if(engine!=null)engine.dialogs=Math.max(0,engine.dialogs-1); immersive();}); d.show(); return d;
    }
    private void toast(String message) { Toast.makeText(this,message,Toast.LENGTH_LONG).show(); }
    void cycleSpeed() { engine.speed = engine.speed==1?engine.fastRate:1; layout.speed.setText(engine.speed+"×"); toast(engine.speed==1?"Velocidade normal":"Avanço rápido "+engine.speed+"× · áudio silenciado"); }
    interface FileAction { String run() throws Exception; }
    private void fileOperation(String label,FileAction action,Runnable success){
        if(busy){toast("Aguarde a operação atual.");return;}
        final long requested=System.nanoTime();
        busy=true;engine.operations++;layout.clearKeys();layout.transferStatus(label);
        final Engine target=engine;
        files.execute(()->{
            long start=requested;String message;boolean ok;
            try{message=action.run();ok=true;}catch(Exception ex){message="Falha: "+ex.getMessage();ok=false;}
            final boolean done=ok;final String result=message;
            android.util.Log.i("GBA-State",label+" "+((System.nanoTime()-start)/1000000)+"ms "+(ok?"OK":"FAIL"));
            ui.post(()->{target.operations=Math.max(0,target.operations-1);busy=false;
                if(isDestroyed()||isFinishing())return;
                layout.clearKeys();picker=false;engine.resumed=activityResumed&&inGame;
                layout.transferStatus(done?result:"Falha na operação");
                ui.postDelayed(()->{if(!busy&&layout!=null)layout.transferStatus(null);},1800);
                if(done){if(success!=null)success.run();}else toast(result);
            });
        });
    }
    private void scheduleCheckpoint(){
        if(checkpointQueued||files.isShutdown())return;
        checkpointQueued=true;final Engine target=engine;target.operations++;
        files.execute(()->{boolean ok=target.checkpoint();ui.post(()->{
            checkpointQueued=false;target.operations=Math.max(0,target.operations-1);
            if(!ok&&!isDestroyed())toast("Não foi possível salvar a sessão automática.");
        });});
    }
    void quickSave() {
        if(!sessionStarted)return;
        fileOperation("Salvando…",()->{backup(engine.slot(0));
            if(!engine.state(0,true)||!engine.flush())throw new IOException("Não foi possível gravar o automático.");
            return "Automático salvo";
        },null);
    }
    void loadAutomatic(){
        if(!engine.slot(0).isFile()){toast("Nenhum estado automático. Use Importar estado.");return;}
        loadSlot(0);
    }
    void saveManual(){
        if(!sessionStarted){toast("Abra o jogo antes de salvar um estado.");return;}
        if(engine.slot(selectedSlot).exists())show(new AlertDialog.Builder(this).setTitle("Salvar no slot "+selectedSlot+"?")
            .setPositiveButton("Salvar",(d,w)->saveSlot(selectedSlot)).setNegativeButton("Cancelar",null));
        else saveSlot(selectedSlot);
    }
    void enterGame(){if(!inGame){inGame=true;sessionStarted=true;setContentView(layout);}engine.resumed=activityResumed&&!picker;immersive();}
    void playProgress(){
        fileOperation("Abrindo progresso…",()->{
            File save=new File(folder,BASE+".sav"),snapshot=new File(getCacheDir(),"carregar-progresso.sav");
            try{
                if(save.isFile()&&save.length()>0){
                    Files.copy(save.toPath(),snapshot.toPath(),StandardCopyOption.REPLACE_EXISTING);
                    if(sessionStarted&&!engine.checkpoint())throw new IOException("Não foi possível guardar a sessão atual.");
                    if(!engine.importSave(snapshot))throw new IOException("Não foi possível carregar o .sav.");
                }
                return "Progresso carregado";
            }finally{snapshot.delete();}
        },this::enterGame);
    }
    void importFromHome(boolean state){importSlot=0;pick(state?IMPORT_STATE:IMPORT_SAVE);}
    void exportProgress(){File save=new File(folder,BASE+".sav");if(!sessionStarted&&(!save.isFile()||save.length()==0)){toast("Importe ou inicie o jogo antes de exportar o progresso.");return;}pick(EXPORT_SAVE);}
    void exportAll(){pick(EXPORT);}
    private void backup(File file) throws IOException {
        if(!file.isFile())return;
        File backups=new File(folder,"backups"); if(!backups.isDirectory()&&!backups.mkdirs())throw new IOException("Falha ao criar backups.");
        File target=new File(backups,new SimpleDateFormat("yyyyMMdd-HHmmss-SSS",Locale.US).format(new Date())+"-"+file.getName());
        Files.copy(file.toPath(),target.toPath());
    }
    private void saveSlot(int slot) {
        fileOperation("Salvando slot "+slot+"…",()->{backup(engine.slot(slot));
            if(!engine.state(slot,true))throw new IOException("Não foi possível gravar o estado.");
            return "Slot "+slot+" salvo";
        },null);
    }
    private String slotLabel(int n) {
        File f=engine.slot(n); String name=n==0?"Automático":"Slot "+n;
        if(!f.isFile())return name+" · vazio";
        return name+" · "+new SimpleDateFormat("dd/MM HH:mm",Locale.getDefault()).format(new Date(f.lastModified()));
    }
    void chooseLoad() {
        String[] items=new String[10]; for(int i=0;i<10;i++)items[i]=slotLabel(i);
        show(new AlertDialog.Builder(this).setTitle("Carregar estado")
            .setItems(items,(d,n)->{
                if(!engine.slot(n).isFile()){toast("Esse slot está vazio.");return;}
                show(new AlertDialog.Builder(this).setTitle("Carregar "+(n==0?"automático":"slot "+n)+"?")
                    .setMessage("A sessão atual será guardada em um backup antes de carregar o estado escolhido.")
                    .setPositiveButton("Carregar",(dialog,w)->loadSlot(n)).setNegativeButton("Cancelar",null));
            }).setNegativeButton("Voltar",null));
    }
    private void loadSlot(int n) {
        fileOperation("Carregando…",()->{
            if(sessionStarted){
                File recovery=new File(folder,"antes-de-carregar.ss");
                if(!engine.state(90,true))throw new IOException("Não foi possível guardar a sessão atual.");
                Files.move(engine.slot(90).toPath(),recovery.toPath(),StandardCopyOption.REPLACE_EXISTING);
            }
            if(!engine.state(n,false))throw new IOException("Estado incompatível ou danificado.");
            return n==0?"Automático carregado":"Slot "+n+" carregado";
        },this::enterGame);
    }
    void menu() { if(playerMenu!=null&&playerMenu.isShowing())return;playerMenu=new PlayerMenu(this,settings,engine,layout);playerMenu.show(); }
    void menuAction(int n) {
            switch(n){
                case 0: chooseSlot(); break;
                case 1: fileOperation("Gravando progresso…",()->{if(!engine.flush())throw new IOException("Falha ao gravar o .sav.");return "Progresso gravado";},null);break;
                case 2: playProgress(); break;
                case 3: confirmImport(false); break;
                case 4: confirmImport(true); break;
                case 5: pick(EXPORT); break;
                case 6: engine.muted=!engine.muted;getPreferences(0).edit().putBoolean("mute",engine.muted).apply();break;
                case 7: show(new AlertDialog.Builder(this).setTitle("Pasta do jogo").setMessage(folder.getAbsolutePath()+"\n\nROM, .sav e .ss0 a .ss9 ficam juntos. originais contém os saves trazidos do computador. backups guarda estados substituídos.\n\nO Android pode restringir o acesso a Android/data. Use Exportar para copiar os saves para uma pasta escolhida. Desinstalar o app remove a pasta; exporte antes.").setPositiveButton("OK",null));break;
                case 8: show(new AlertDialog.Builder(this).setTitle(spec.title).setMessage("Núcleo mGBA 0.10.5 nativo (MPL-2.0).\n\nD-pad multitoque, A/B/L/R/Start/Select, velocidade configurável até 8×, estados mGBA, save de progresso e áudio estéreo.\n\nSem login, token, serviços Google ou permissão de internet. Licenças completas incluídas em assets/licenses e no projeto.").setPositiveButton("OK",null));break;
                case 9: fileOperation("Salvando sessão…",()->{if(!engine.checkpoint())throw new IOException("Libere espaço e tente novamente.");return "Sessão salva";},this::finish);break;
            }
    }
    private void chooseSlot() {
        String[] items=new String[9];for(int i=0;i<9;i++)items[i]=slotLabel(i+1);
        show(new AlertDialog.Builder(this).setTitle("Slot manual").setSingleChoiceItems(items,selectedSlot-1,(d,n)->{selectedSlot=n+1;getPreferences(0).edit().putInt("slot",selectedSlot).apply();d.dismiss();toast("Slot manual "+selectedSlot);}).setNegativeButton("Voltar",null));
    }
    private void confirmImport(boolean state) {
        show(new AlertDialog.Builder(this).setTitle(state?"Importar save state":"Importar progresso")
            .setMessage(state?"Escolha um estado mGBA (.ss ou PNG). Ele será carregado e copiado para o automático. O estado anterior será guardado em backups.":"Escolha um arquivo .sav compatível com este jogo (SRAM, EEPROM ou Flash). O jogo será reiniciado pelo save importado. O save atual será guardado em backups.")
            .setPositiveButton("Escolher arquivo",(d,w)->{importSlot=0;pick(state?IMPORT_STATE:IMPORT_SAVE);}).setNegativeButton("Cancelar",null));
    }
    private void pick(int request) {
        if(busy){toast("Aguarde a operação atual.");return;}
        picker=true; engine.resumed=false;
        Intent intent;
        if(request==EXPORT_SAVE){intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/octet-stream").putExtra(Intent.EXTRA_TITLE,BASE+".sav");}
        else if(request==EXPORT){intent=new Intent(Intent.ACTION_CREATE_DOCUMENT).setType("application/zip").putExtra(Intent.EXTRA_TITLE,spec.folder+"-saves-"+new SimpleDateFormat("yyyyMMdd-HHmmss",Locale.US).format(new Date())+".zip");}
        else intent=new Intent(Intent.ACTION_OPEN_DOCUMENT).setType("*/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        try{startActivityForResult(intent,request);}catch(ActivityNotFoundException ex){picker=false;engine.resumed=activityResumed&&inGame;toast("Nenhum seletor de arquivos disponível.");}
    }
    @Override protected void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(result!=RESULT_OK||data==null||data.getData()==null){picker=false;engine.resumed=activityResumed&&inGame;return;}
        Uri uri=data.getData();
        fileOperation(request==IMPORT_SAVE||request==IMPORT_STATE?"Importando…":"Exportando…",()->{
            String message;
                if(request==EXPORT_SAVE){
                    if(!engine.flush())throw new IOException("Falha ao gravar o .sav.");
                    try(InputStream in=new FileInputStream(new File(folder,BASE+".sav"));OutputStream out=getContentResolver().openOutputStream(uri,"wt")){
                        if(out==null)throw new IOException("Destino indisponível.");copy(in,out);
                    }
                    message="Progresso .sav exportado.";
                }else if(request==EXPORT){
                    if(!(sessionStarted?engine.checkpoint():engine.flush()))throw new IOException("Falha ao gravar a sessão antes da exportação.");
                    try(OutputStream raw=getContentResolver().openOutputStream(uri,"wt")){if(raw==null)throw new IOException("Destino indisponível.");try(ZipOutputStream zip=new ZipOutputStream(raw)){zipFolder(folder,folder,zip);}}
                    message="Saves e estados exportados.";
                }else{
                    File temp=new File(getCacheDir(),"import-save.tmp");
                    try(InputStream in=getContentResolver().openInputStream(uri);OutputStream out=new FileOutputStream(temp)){
                        if(in==null)throw new IOException("Arquivo indisponível.");
                        byte[] bytes=new byte[16384]; int n,total=0;while((n=in.read(bytes))!=-1){total+=n;if(total>4*1024*1024)throw new IOException("Arquivo grande demais para um save.");out.write(bytes,0,n);}
                    }
                    if(!(sessionStarted?engine.checkpoint():engine.flush()))throw new IOException("Não foi possível guardar a sessão antes da importação.");
                    if(request==IMPORT_SAVE){backup(new File(folder,BASE+".sav"));if(!engine.importSave(temp))throw new IOException("Save inválido. Use um .sav compatível com este jogo.");message="Progresso importado. Escolha Continue no jogo.";}
                    else{
                        backup(engine.slot(importSlot));
                        synchronized(engine.lock){
                            File old=engine.slot(91);Files.copy(temp.toPath(),old.toPath(),StandardCopyOption.REPLACE_EXISTING);
                            if(!engine.state(91,false)){old.delete();throw new IOException("Estado incompatível. Use save state mGBA desta ROM.");}
                            Files.move(old.toPath(),engine.slot(importSlot).toPath(),StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);
                        }
                        message="Estado importado e carregado no slot "+importSlot+".";
                    }
                    temp.delete();
                }
            return message;
        },()->{if(request==IMPORT_SAVE||request==IMPORT_STATE)enterGame();});
    }
    private void zipFolder(File root,File current,ZipOutputStream zip)throws IOException{
        File[] files=current.listFiles();if(files==null)throw new IOException("Não foi possível listar a pasta.");
        for(File f:files){if(f.isDirectory()){zipFolder(root,f,zip);continue;}String name=f.getName();if(name.endsWith(".gba")||name.endsWith(".working")||name.endsWith(".tmp"))continue;
            zip.putNextEntry(new ZipEntry(root.toPath().relativize(f.toPath()).toString()));try(InputStream in=new FileInputStream(f)){copy(in,zip);}zip.closeEntry();}
    }

    String gameTitle(){return spec==null?"GBA":spec.title;}
    void applyOrientation(){int n=settings.number("orientation",1);setRequestedOrientation(n==1?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE:n==2?android.content.pm.ActivityInfo.SCREEN_ORIENTATION_SENSOR_PORTRAIT:android.content.pm.ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);}
    void applyAwake(){if(settings.enabled("keepAwake",true))getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);}
    AlertDialog showPanel(View view){return show(new AlertDialog.Builder(this).setView(view));}
    void editHud(){if(!layout.isEditing()){engine.dialogs++;layout.editing(true);}}
    void finishHudEdit(){if(layout.isEditing()){layout.editing(false);engine.dialogs=Math.max(0,engine.dialogs-1);toast("Layout salvo para esta orientação.");}}
    void resetHud(){show(new AlertDialog.Builder(this).setTitle("Restaurar este layout?").setMessage("Só as posições e ajustes desta orientação serão restaurados.").setPositiveButton("Restaurar",(d,w)->{settings.resetLayout(layout.getWidth()>layout.getHeight());layout.refresh();}).setNegativeButton("Cancelar",null));}
    void editHudControl(){
        LinearLayout panel=new LinearLayout(this);panel.setOrientation(LinearLayout.VERTICAL);panel.setPadding(40,20,40,20);
        addHudSlider(panel,"Tamanho",60,180,Math.round(layout.selectedSize()*100),n->layout.changeSelected(n/100f,layout.selectedOpacity()));
        addHudSlider(panel,"Opacidade",15,100,Math.round(layout.selectedOpacity()*100),n->layout.changeSelected(layout.selectedSize(),n/100f));
        Switch visible=new Switch(this);visible.setText("Mostrar este botão");visible.setTextColor(GameLayout.INK);visible.setChecked(layout.selectedVisible());
        visible.setOnCheckedChangeListener((v,checked)->layout.changeSelectedVisible(checked));panel.addView(visible);
        show(new AlertDialog.Builder(this).setTitle(layout.selected()).setView(panel).setPositiveButton("Pronto",null));
    }
    void manageHudButtons(){
        String[] names=layout.controlNames();boolean[] shown=new boolean[names.length];for(int i=0;i<names.length;i++)shown[i]=layout.controlVisible(names[i]);
        show(new AlertDialog.Builder(this).setTitle("Botões do HUD").setMultiChoiceItems(names,shown,(d,n,checked)->layout.setControlVisible(names[n],checked)).setPositiveButton("Pronto",null));
    }
    private void addHudSlider(LinearLayout parent,String label,int min,int max,int current,java.util.function.IntConsumer change){
        TextView title=new TextView(this);title.setText(label+" · "+current+"%");title.setTextSize(16);title.setTextColor(GameLayout.INK);parent.addView(title);
        SeekBar bar=new SeekBar(this);bar.setMax(max-min);bar.setProgress(current-min);bar.setContentDescription(label);parent.addView(bar,new LinearLayout.LayoutParams(-1,layout.dp(56)));
        bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){if(user){title.setText(label+" · "+(n+min)+"%");change.accept(n+min);}}});
    }
    private int mapKey(int key){switch(key){case KeyEvent.KEYCODE_BUTTON_A:case KeyEvent.KEYCODE_X:return 1;case KeyEvent.KEYCODE_BUTTON_B:case KeyEvent.KEYCODE_Z:return 2;case KeyEvent.KEYCODE_BUTTON_SELECT:case KeyEvent.KEYCODE_SPACE:return 4;case KeyEvent.KEYCODE_BUTTON_START:case KeyEvent.KEYCODE_ENTER:return 8;case KeyEvent.KEYCODE_DPAD_RIGHT:return 16;case KeyEvent.KEYCODE_DPAD_LEFT:return 32;case KeyEvent.KEYCODE_DPAD_UP:return 64;case KeyEvent.KEYCODE_DPAD_DOWN:return 128;case KeyEvent.KEYCODE_BUTTON_R1:case KeyEvent.KEYCODE_W:return 256;case KeyEvent.KEYCODE_BUTTON_L1:case KeyEvent.KEYCODE_Q:return 512;default:return 0;}}
    @Override public boolean dispatchKeyEvent(KeyEvent e){if(engine!=null&&engine.dialogs==0&&inGame&&!picker){int mask=mapKey(e.getKeyCode());if(mask!=0){if(e.getAction()==KeyEvent.ACTION_DOWN)engine.input.hardware(mask,true);else if(e.getAction()==KeyEvent.ACTION_UP)engine.input.hardware(mask,false);return true;}}return super.dispatchKeyEvent(e);}
}
