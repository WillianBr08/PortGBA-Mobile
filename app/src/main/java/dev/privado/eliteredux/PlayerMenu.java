package dev.privado.eliteredux;

import android.app.AlertDialog;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.graphics.drawable.RippleDrawable;
import android.content.res.ColorStateList;
import android.view.*;
import android.widget.*;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.function.IntConsumer;

/** Three stable destinations. Android Back returns to settings, then to the game. */
final class PlayerMenu {
    private enum Page { GAME, SAVES, SETTINGS, DISPLAY, CONTROLS, SPEED }
    private final MainActivity main;
    private final Engine engine;
    private final Settings settings;
    private final GameLayout game;
    private LinearLayout content,tabs;
    private TextView heading;
    private Button back;
    private AlertDialog dialog;
    private Page page=Page.GAME;
    PlayerMenu(MainActivity main,Settings settings,Engine engine,GameLayout game){this.main=main;this.settings=settings;this.engine=engine;this.game=game;}
    private int dp(float n){return game.dp(n);}
    boolean isShowing(){return dialog!=null&&dialog.isShowing();}
    void show(){
        LinearLayout shell=new LinearLayout(main);shell.setOrientation(LinearLayout.VERTICAL);shell.setPadding(dp(18),dp(12),dp(18),dp(8));
        shell.setBackground(background(GameLayout.PANEL,24));
        LinearLayout header=new LinearLayout(main);header.setGravity(Gravity.CENTER_VERTICAL);
        back=GameLayout.plainButton(main,"‹ Ajustes");back.setTextSize(13);back.setOnClickListener(v->render(Page.SETTINGS));header.addView(back,new LinearLayout.LayoutParams(dp(84),dp(48)));
        heading=text("Jogo pausado",22,true);header.addView(heading,new LinearLayout.LayoutParams(0,-2,1));
        Button resume=GameLayout.plainButton(main,"Continuar");resume.setTextSize(14);resume.setOnClickListener(v->dialog.dismiss());header.addView(resume,new LinearLayout.LayoutParams(dp(96),dp(48)));shell.addView(header);
        tabs=new LinearLayout(main);tabs.setPadding(0,dp(8),0,dp(12));shell.addView(tabs);
        ScrollView scroll=new ScrollView(main);scroll.setFillViewport(false);content=new LinearLayout(main);content.setOrientation(LinearLayout.VERTICAL);content.setPadding(0,0,0,dp(12));scroll.addView(content);shell.addView(scroll,new LinearLayout.LayoutParams(-1,0,1));
        dialog=main.showPanel(shell);dialog.getWindow().setGravity(Gravity.CENTER);dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        dialog.setOnKeyListener((d,key,event)->{if(key==KeyEvent.KEYCODE_BACK&&event.getAction()==KeyEvent.ACTION_UP){if(isSubpage())render(Page.SETTINGS);else dialog.dismiss();return true;}return key==KeyEvent.KEYCODE_BACK;});
        resize();render(Page.GAME);
    }
    void resize(){if(isShowing())dialog.getWindow().setLayout(Math.min(main.getResources().getDisplayMetrics().widthPixels-dp(16),dp(620)),(int)(main.getResources().getDisplayMetrics().heightPixels*.92));}
    private boolean isSubpage(){return page==Page.DISPLAY||page==Page.CONTROLS||page==Page.SPEED;}
    private GradientDrawable background(int color,int radius){GradientDrawable d=new GradientDrawable();d.setColor(color);d.setCornerRadius(dp(radius));return d;}
    private TextView text(String value,int size,boolean strong){TextView t=new TextView(main);t.setText(value);t.setTextSize(size);t.setTextColor(GameLayout.INK);t.setTypeface(strong?game.bold:game.regular);return t;}
    private void render(Page next){
        page=next;content.removeAllViews();tabs.removeAllViews();back.setVisibility(isSubpage()?View.VISIBLE:View.GONE);
        String title=page==Page.DISPLAY?"Tela e som":page==Page.CONTROLS?"Controles":page==Page.SPEED?"Velocidade":"Jogo pausado";heading.setText(title);
        String[] labels={"Jogo","Saves","Ajustes"};Page[] roots={Page.GAME,Page.SAVES,Page.SETTINGS};
        for(int i=0;i<3;i++){
            final Page target=roots[i];boolean active=page==target||(isSubpage()&&target==Page.SETTINGS);Button tab=GameLayout.plainButton(main,labels[i]);tab.setSelected(active);tab.setTypeface(game.bold);tab.setTextColor(active?GameLayout.BG:GameLayout.INK);tab.setBackground(background(active?GameLayout.ACCENT:GameLayout.BG,12));tab.setOnClickListener(v->render(target));
            LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)p.leftMargin=dp(8);tabs.addView(tab,p);
        }
        switch(page){case GAME:home();break;case SAVES:saves();break;case SETTINGS:settings();break;case DISPLAY:display();break;case CONTROLS:controls();break;case SPEED:speed();break;}
    }
    private void section(String title){TextView v=text(title,13,true);v.setTextColor(GameLayout.ACCENT);v.setPadding(dp(4),dp(12),0,dp(8));content.addView(v);}
    private void note(String value){TextView v=text(value,13,false);v.setTextColor(GameLayout.MUTED);v.setPadding(dp(4),dp(8),dp(4),dp(10));content.addView(v);}
    private void row(String title,String detail,Runnable action){
        LinearLayout box=new LinearLayout(main);box.setOrientation(LinearLayout.VERTICAL);box.setPadding(dp(14),dp(12),dp(14),dp(12));box.setMinimumHeight(dp(64));box.setFocusable(true);box.setClickable(true);box.setBackground(new RippleDrawable(ColorStateList.valueOf(0x44ffbf69),background(GameLayout.BG,14),null));
        box.addView(text(title,16,true));if(!detail.isEmpty()){TextView hint=text(detail,12,false);hint.setTextColor(GameLayout.MUTED);box.addView(hint);}box.setOnClickListener(v->action.run());LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(-1,-2);p.bottomMargin=dp(7);content.addView(box,p);
    }
    private void actions(String first,Runnable one,String second,Runnable two){
        LinearLayout row=new LinearLayout(main);Button a=GameLayout.plainButton(main,first),b=GameLayout.plainButton(main,second);a.setBackground(background(GameLayout.BG,14));b.setBackground(background(GameLayout.BG,14));a.setTextSize(14);b.setTextSize(14);a.setOnClickListener(v->{dialog.dismiss();one.run();});b.setOnClickListener(v->{dialog.dismiss();two.run();});row.addView(a,new LinearLayout.LayoutParams(0,dp(56),1));LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(56),1);p.leftMargin=dp(8);row.addView(b,p);content.addView(row);
    }
    private void toggle(String title,String detail,String key,boolean def,Runnable changed){
        LinearLayout line=new LinearLayout(main);line.setGravity(Gravity.CENTER_VERTICAL);line.setPadding(dp(4),dp(10),dp(4),dp(10));LinearLayout labels=new LinearLayout(main);labels.setOrientation(LinearLayout.VERTICAL);labels.addView(text(title,16,true));TextView hint=text(detail,12,false);hint.setTextColor(GameLayout.MUTED);labels.addView(hint);line.addView(labels,new LinearLayout.LayoutParams(0,-2,1));
        Switch control=new Switch(main);control.setContentDescription(title);control.setMinimumHeight(dp(48));control.setChecked(settings.enabled(key,def));control.setOnCheckedChangeListener((v,checked)->{settings.put(key,checked);settings.apply(engine);changed.run();});line.addView(control);line.setOnClickListener(v->control.toggle());content.addView(line);
    }
    private void slider(String label,int min,int max,int current,IntConsumer change){
        TextView title=text(label+" · "+current+"%",15,true);title.setPadding(dp(4),dp(12),0,0);content.addView(title);SeekBar bar=new SeekBar(main);bar.setMax(max-min);bar.setProgress(current-min);bar.setContentDescription(label);bar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener(){public void onStartTrackingTouch(SeekBar b){}public void onStopTrackingTouch(SeekBar b){}public void onProgressChanged(SeekBar b,int n,boolean user){if(user){title.setText(label+" · "+(n+min)+"%");change.accept(n+min);}}});content.addView(bar,new LinearLayout.LayoutParams(-1,dp(48)));
    }
    private void options(String title,String[] labels,int[] values,int selected,IntConsumer change){
        int checked=0;for(int i=0;i<values.length;i++)if(values[i]==selected)checked=i;
        main.show(new AlertDialog.Builder(main).setTitle(title).setSingleChoiceItems(labels,checked,(d,n)->{change.accept(values[n]);d.dismiss();render(page);}).setNegativeButton("Cancelar",null));
    }
    private void orientation(){
        section("Orientação da tela");LinearLayout choices=new LinearLayout(main);String[] names={"Paisagem","Retrato","Automática"};int[] values={1,2,0};int selected=settings.number("orientation",1);
        for(int i=0;i<3;i++){final int value=values[i];Button button=GameLayout.plainButton(main,names[i]);button.setSelected(value==selected);button.setTextSize(13);button.setTextColor(value==selected?GameLayout.BG:GameLayout.INK);button.setBackground(background(value==selected?GameLayout.ACCENT:GameLayout.BG,12));button.setOnClickListener(v->{settings.put("orientation",value);main.applyOrientation();render(page);});LinearLayout.LayoutParams p=new LinearLayout.LayoutParams(0,dp(48),1);if(i>0)p.leftMargin=dp(6);choices.addView(button,p);}content.addView(choices);
    }
    private void action(int n){dialog.dismiss();main.menuAction(n);}
    private void home(){
        note(main.gameTitle());actions("Salvar automático",main::quickSave,"Carregar automático",main::loadAutomatic);orientation();
        section("Atalhos");row("Editar os botões","Mover, ocultar, redimensionar e ajustar transparência",()->{dialog.dismiss();main.editHud();});row("Salvar e sair","Guarda o estado automático antes de fechar",()->action(9));
    }
    private void saves(){
        section("Estado automático");String date=engine.slot(0).isFile()?new SimpleDateFormat("dd/MM · HH:mm",Locale.getDefault()).format(new Date(engine.slot(0).lastModified())):"Ainda não existe um estado automático";note(date);actions("Salvar agora",main::quickSave,"Carregar",main::loadAutomatic);
        section("Importar e exportar");note("Progresso (.sav) é o SAVE do jogo. Estado é uma captura instantânea da partida.");
        row("Importar progresso (.sav)","Abrir um save de outro emulador",()->{dialog.dismiss();main.importFromHome(false);});row("Exportar progresso (.sav)","Guardar somente o progresso",()->{dialog.dismiss();main.exportProgress();});row("Importar estado","Arquivo mGBA .ss ou PNG → automático",()->{dialog.dismiss();main.importFromHome(true);});row("Exportar todos os saves","ZIP com estados, progresso e backups",()->{dialog.dismiss();main.exportAll();});
        section("Mais opções");row("Carregar outro slot","Automático e nove slots manuais",()->{dialog.dismiss();main.chooseLoad();});row("Salvar no slot manual","Escolher destino em Ajustes → Gravação",()->{dialog.dismiss();main.saveManual();});row("Iniciar pelo progresso (.sav)","Reiniciar o jogo usando o arquivo de progresso",()->{dialog.dismiss();main.playProgress();});
    }
    private void settings(){
        row("Tela e som","Orientação, imagem, áudio e FPS",()->render(Page.DISPLAY));row("Controles","Layout, tamanho, transparência e vibração",()->render(Page.CONTROLS));row("Velocidade","Avanço rápido e renderização",()->render(Page.SPEED));
        section("Gravação");toggle("Salvar ao sair","Também ao trocar de aplicativo","autoState",true,()->{});row("Slot manual","Destino separado do automático do HUD",()->action(0));
        row("Intervalo do progresso",settings.number("saveInterval",10)+" segundos",()->options("Gravar progresso a cada",new String[]{"5 segundos","10 segundos","30 segundos","60 segundos"},new int[]{5,10,30,60},settings.number("saveInterval",10),n->{settings.put("saveInterval",n);settings.apply(engine);}));
        section("Aplicativo");row("Pasta dos saves","Localização e backups",()->action(7));row("Sobre e licenças","Núcleo e fontes do aplicativo",()->action(8));
    }
    private void display(){
        orientation();section("Imagem");String[] modes={"Original · 3:2","Tela inteira · esticada","Pixels inteiros"};row("Proporção",modes[settings.number("videoMode",0)],()->options("Proporção",modes,new int[]{0,1,2},settings.number("videoMode",0),n->{settings.put("videoMode",n);game.refresh();}));toggle("Mostrar FPS","Contador sobre a tela","showFps",false,game::refresh);toggle("Manter tela acesa","Enquanto estiver no jogo","keepAwake",true,main::applyAwake);
        section("Áudio");toggle("Silenciar","Avanço rápido sempre fica sem som","muted",false,()->{});slider("Volume",0,100,settings.number("volume",80),n->{settings.put("volume",n);settings.apply(engine);});
    }
    private void controls(){
        row("Editar na tela","Arraste e toque em cada botão para personalizar",()->{dialog.dismiss();main.editHud();});row("Botões visíveis","Retirar ou restaurar controles do HUD",()->{dialog.dismiss();main.manageHudButtons();});slider("Tamanho geral",70,150,Math.round(settings.value("hudScale",1)*100),n->{settings.put("hudScale",n/100f);game.refresh();});slider("Opacidade",15,100,Math.round(settings.value("hudOpacity",.65f)*100),n->{settings.put("hudOpacity",n/100f);game.refresh();});toggle("Mostrar HUD","Voltar do Android abre este menu","showHud",true,game::refresh);toggle("Vibrar ao tocar","Resposta curta nos comandos","haptic",false,()->{});row("Restaurar layout","Somente a orientação atual",()->{dialog.dismiss();main.resetHud();});
    }
    private void speed(){
        row("Avanço rápido",settings.number("fastRate",4)+"×",()->options("Avanço rápido",new String[]{"2×","3×","4×","6×","8×"},new int[]{2,3,4,6,8},settings.number("fastRate",4),n->{settings.put("fastRate",n);settings.apply(engine);if(engine.speed>1){engine.speed=n;game.speed.setText(n+"×");}}));
        row("Pular renderização",new String[]{"Desativado","Um quadro","Dois quadros"}[settings.number("frameskip",0)],()->options("Pular renderização",new String[]{"Desativado","Um quadro","Dois quadros"},new int[]{0,1,2},settings.number("frameskip",0),n->{settings.put("frameskip",n);settings.apply(engine);}));note("Os comandos são lidos a cada quadro. A velocidade alcançada depende do aparelho e da cena.");
    }
}
