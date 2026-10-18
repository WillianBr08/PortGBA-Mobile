package dev.privado.eliteredux;

import android.content.Context;
import android.graphics.*;
import android.graphics.drawable.*;
import android.view.*;
import android.widget.*;
import java.util.*;

final class GameLayout extends ViewGroup {
    static final int BG=0xff0d1117, PANEL=0xff17212c, INK=0xfff3f0e8, ACCENT=0xffffbf69, MUTED=0xffa1aebc;
    final SurfaceView screen;
    final Button speed;
    final TextView status;
    private final MainActivity activity;
    private final Engine engine;
    private final Settings settings;
    private final LinkedHashMap<String,View> controls=new LinkedHashMap<>();
    private final Paint paint=new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF video=new RectF();
    private final LinearLayout editor;
    private final Pad pad;
    private final float density;
    private int buttonKeys,padKeys;
    private boolean editing, editorAtBottom;
    private String selected="A";
    private float downX,downY,startX,startY;
    private boolean moved;
    private String transfer,lastStatus="";
    final Typeface regular,bold;
    GameLayout(MainActivity context,Engine engine,Settings settings) {
        super(context);activity=context;this.engine=engine;this.settings=settings;
        density=getResources().getDisplayMetrics().density;
        regular=Typeface.createFromAsset(context.getAssets(),"fonts/Outfit-Regular.ttf");
        bold=Typeface.createFromAsset(context.getAssets(),"fonts/Outfit-Bold.ttf");
        setBackgroundColor(BG);setClipChildren(true);setMotionEventSplittingEnabled(true);setWillNotDraw(false);
        screen=new SurfaceView(context);screen.setContentDescription("Tela do jogo");addView(screen);
        screen.getHolder().setFixedSize(240,160);
        screen.getHolder().addCallback(new SurfaceHolder.Callback(){
            public void surfaceCreated(SurfaceHolder h){engine.surface(h.getSurface());}
            public void surfaceChanged(SurfaceHolder h,int f,int w,int height){engine.surface(h.getSurface());}
            public void surfaceDestroyed(SurfaceHolder h){engine.surface(null);}
        });
        pad=new Pad(context);addControl("D-pad",pad);
        addKey("A",1);addKey("B",2);addKey("L",512);addKey("R",256);addKey("SELECT",4);addKey("START",8);
        addAction("MENU",context::menu);speed=addAction("1×",context::cycleSpeed);
        addAction("SALVAR",context::quickSave);addAction("CARREGAR",context::loadAutomatic);
        status=new TextView(context);status.setTextColor(INK);status.setTextSize(11);status.setTypeface(regular);status.setGravity(Gravity.CENTER);
        GradientDrawable info=new GradientDrawable();info.setColor(0xcc0d1117);info.setCornerRadius(dp(12));status.setBackground(info);addView(status);
        editor=new LinearLayout(context);editor.setElevation(dp(12));editor.setClickable(true);editor.setOrientation(LinearLayout.VERTICAL);editor.setPadding(dp(16),dp(12),dp(16),dp(12));
        GradientDrawable panel=new GradientDrawable();panel.setColor(0xf517212c);panel.setCornerRadius(dp(20));panel.setStroke(dp(1),0xff435365);editor.setBackground(panel);
        TextView title=new TextView(context);title.setText("Seu controle. Seu jeito.");title.setTypeface(bold);title.setTextSize(19);title.setTextColor(INK);editor.addView(title);
        TextView hint=new TextView(context);hint.setText("Arraste os botões. Toque para ajustar tamanho e opacidade.");hint.setTextSize(12);hint.setTextColor(MUTED);editor.addView(hint);
        LinearLayout row=new LinearLayout(context);Button move=plainButton(context,"Mover painel"),reset=plainButton(context,"Restaurar"),done=plainButton(context,"Concluir");
        move.setOnClickListener(v->{editorAtBottom=!editorAtBottom;requestLayout();});reset.setOnClickListener(v->activity.resetHud());done.setOnClickListener(v->activity.finishHudEdit());
        row.addView(move,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(reset,new LinearLayout.LayoutParams(0,dp(48),1));row.addView(done,new LinearLayout.LayoutParams(0,dp(48),1));editor.addView(row);addView(editor);editor.setVisibility(GONE);
    }
    int dp(float n){return Math.round(n*density);}
    private boolean wide(){return isAttachedToWindow()&&getWidth()>0?getWidth()>getHeight():getResources().getConfiguration().orientation==android.content.res.Configuration.ORIENTATION_LANDSCAPE;}
    static Button plainButton(Context c,String text){Button v=new Button(c);v.setText(text);v.setAllCaps(false);v.setTextColor(ACCENT);v.setTextSize(15);v.setBackgroundColor(Color.TRANSPARENT);return v;}
    private void addControl(String name,View view){controls.put(name,view);view.setTag(name);addView(view);}
    private Button baseButton(String name){
        Button v=new Button(activity);v.setText(name);v.setAllCaps(false);v.setTextColor(INK);v.setTypeface(bold);v.setTextSize(name.length()>3?11:20);v.setPadding(0,0,0,0);v.setMinWidth(0);v.setMinimumWidth(0);v.setMinHeight(0);v.setMinimumHeight(0);v.setElevation(dp(1));
        boolean action=name.length()>3 || name.equals("1×");
        GradientDrawable normal=new GradientDrawable();normal.setColor(0x66202a35);normal.setCornerRadius(dp(action?16:100));normal.setStroke(dp(1.2f),0xddf3f0e8);
        GradientDrawable pressed=new GradientDrawable();pressed.setColor(0xddffbf69);pressed.setCornerRadius(dp(action?16:100));pressed.setStroke(dp(2),ACCENT);
        StateListDrawable drawable=new StateListDrawable();drawable.addState(new int[]{android.R.attr.state_pressed},pressed);drawable.addState(new int[]{},normal);v.setBackground(drawable);
        v.setContentDescription(name.equals("1×")?"Alternar avanço rápido":name);return v;
    }
    private void addKey(String name,int mask){
        Button v=baseButton(name);v.setOnClickListener(w->{});
        v.setOnTouchListener((view,event)->{
            if(editing)return editTouch(name,view,event);
            int action=event.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){buttonKeys|=mask;v.setPressed(true);haptic(v);}
            else if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL){buttonKeys&=~mask;v.setPressed(false);if(action==MotionEvent.ACTION_UP)v.performClick();}
            engine.input.touch(buttonKeys|padKeys);return true;
        });addControl(name,v);
    }
    private Button addAction(String name,Runnable action){
        Button v=baseButton(name);v.setOnClickListener(w->{if(!editing)action.run();});
        v.setOnTouchListener((view,event)->editing&&editTouch(name,view,event));addControl(name,v);return v;
    }
    private void haptic(View v){if(settings.enabled("haptic",false))v.performHapticFeedback(HapticFeedbackConstants.VIRTUAL_KEY);}
    void clearKeys(){buttonKeys=padKeys=0;engine.input.clear();for(View v:controls.values())v.setPressed(false);pad.invalidate();}
    void editing(boolean edit){editing=edit;clearKeys();editor.setVisibility(edit?VISIBLE:GONE);requestLayout();invalidate();}
    boolean isEditing(){return editing;}
    void refresh(){requestLayout();invalidate();}
    void transferStatus(String text){transfer=text;updateStatus(lastStatus);}
    void updateStatus(String text){lastStatus=text;status.setText(transfer!=null?transfer:text);status.setVisibility(!editing&&(transfer!=null||settings.enabled("showFps",false))?VISIBLE:GONE);}
    private String key(String button,String property){return settings.layoutKey(wide(),button,property);}
    String[] controlNames(){return controls.keySet().toArray(new String[0]);}
    boolean controlVisible(String name){return settings.enabled(key(name,"visible"),true);}
    void setControlVisible(String name,boolean shown){settings.put(key(name,"visible"),shown);refresh();}
    boolean selectedVisible(){return controlVisible(selected);}
    void changeSelectedVisible(boolean shown){setControlVisible(selected,shown);}
    void changeSelected(float size,float opacity){settings.put(key(selected,"size"),size);settings.put(key(selected,"opacity"),opacity);refresh();}
    float selectedSize(){return settings.value(key(selected,"size"),1);}
    float selectedOpacity(){return settings.value(key(selected,"opacity"),1);}
    String selected(){return selected;}
    private boolean editTouch(String name,View view,MotionEvent event){
        switch(event.getActionMasked()){
            case MotionEvent.ACTION_DOWN:selected=name;downX=event.getRawX();downY=event.getRawY();startX=view.getX();startY=view.getY();moved=false;view.setPressed(true);invalidate();break;
            case MotionEvent.ACTION_MOVE:
                float dx=event.getRawX()-downX,dy=event.getRawY()-downY;if(Math.abs(dx)+Math.abs(dy)>dp(5))moved=true;
                view.setX(Math.max(0,Math.min(getWidth()-view.getWidth(),startX+dx)));view.setY(Math.max(0,Math.min(getHeight()-view.getHeight(),startY+dy)));invalidate();break;
            case MotionEvent.ACTION_UP:
                view.setPressed(false);settings.put(key(name,"x"),(view.getX()+view.getWidth()/2f)/getWidth());settings.put(key(name,"y"),(view.getY()+view.getHeight()/2f)/getHeight());
                if(!moved)activity.editHudControl();invalidate();break;
            case MotionEvent.ACTION_CANCEL:view.setPressed(false);break;
        }return true;
    }
    private void place(View v,int x,int y,int w,int h){v.measure(MeasureSpec.makeMeasureSpec(Math.max(1,w),MeasureSpec.EXACTLY),MeasureSpec.makeMeasureSpec(Math.max(1,h),MeasureSpec.EXACTLY));v.layout(x,y,x+w,y+h);}
    @Override protected void onMeasure(int w,int h){setMeasuredDimension(MeasureSpec.getSize(w),MeasureSpec.getSize(h));}
    @Override protected void onLayout(boolean changed,int l,int t,int r,int b){
        int w=getWidth(),h=getHeight(),mode=settings.number("videoMode",0);float sw=w,sh=h;
        if(mode!=1){float factor=Math.min(w/240f,h/160f);if(mode==2&&factor>=1)factor=(float)Math.floor(factor);sw=240*factor;sh=160*factor;}
        video.set((w-sw)/2,(h-sh)/2,(w+sw)/2,(h+sh)/2);place(screen,(int)video.left,(int)video.top,(int)video.width(),(int)video.height());
        float global=settings.value("hudScale",1),alpha=settings.value("hudOpacity",.65f);boolean visible=settings.enabled("showHud",true);
        for(Map.Entry<String,View> entry:controls.entrySet()){
            String name=entry.getKey();View view=entry.getValue();boolean action=name.length()>3&&!name.equals("D-pad"),dpad=name.equals("D-pad");
            float size=global*settings.value(key(name,"size"),1);int bw=dp(dpad?132:action?80:name.equals("1×")?56:60),bh=dp(dpad?132:action?48:name.equals("1×")?48:60);
            if(name.equals("L")||name.equals("R")){bw=dp(80);bh=dp(48);}bw=Math.max(dp(48),(int)(bw*size));bh=Math.max(dp(48),(int)(bh*size));bw=Math.min(w,bw);bh=Math.min(h,bh);
            float[] pos=defaultPosition(name);float cx=settings.value(key(name,"x"),pos[0]),cy=settings.value(key(name,"y"),pos[1]);
            int x=Math.max(0,Math.min(w-bw,(int)(cx*w-bw/2f))),y=Math.max(0,Math.min(h-bh,(int)(cy*h-bh/2f)));
            place(view,x,y,bw,bh);view.setTranslationX(0);view.setTranslationY(0);view.setAlpha(editing?.95f:Math.max(name.equals("MENU")?.4f:.12f,alpha*settings.value(key(name,"opacity"),1)));
            view.setVisibility(editing||(visible&&controlVisible(name))?VISIBLE:GONE);
        }
        place(status,w/2-dp(72),dp(8),dp(144),dp(28));status.setVisibility(!editing&&(transfer!=null||settings.enabled("showFps",false))?VISIBLE:GONE);
        int ew=Math.min(w-dp(24),dp(440));place(editor,(w-ew)/2,editorAtBottom?h-dp(138):dp(12),ew,dp(126));
    }
    private float[] defaultPosition(String key){
        float inset=dp(32)/(float)Math.max(1,getHeight());boolean wide=wide();
        switch(key){
            case "MENU":return new float[]{.08f,Math.max(inset,.08f)};
            case "1×":return new float[]{.92f,Math.max(inset,.08f)};
            case "SALVAR":return new float[]{.38f,Math.max(inset,.08f)};
            case "CARREGAR":return new float[]{.62f,Math.max(inset,.08f)};
            case "D-pad":return new float[]{wide?.22f:.20f,wide?.72f:.66f};
            case "A":return new float[]{wide?.80f:.86f,wide?.65f:.60f};
            case "B":return new float[]{wide?.70f:.67f,wide?.80f:.69f};
            case "L":return new float[]{wide?.13f:.13f,wide?.28f:.38f};
            case "R":return new float[]{wide?.87f:.87f,wide?.28f:.38f};
            case "SELECT":return new float[]{.38f,wide?.91f:.79f};
            default:return new float[]{.62f,wide?.91f:.79f};
        }
    }
    @Override protected void dispatchDraw(Canvas c){
        super.dispatchDraw(c);
        if(editing){paint.setStrokeWidth(dp(1));paint.setColor(0x284f7286);for(int x=0;x<getWidth();x+=dp(24))c.drawLine(x,0,x,getHeight(),paint);for(int y=0;y<getHeight();y+=dp(24))c.drawLine(0,y,getWidth(),y,paint);
            View v=controls.get(selected);if(v!=null){paint.setStyle(Paint.Style.STROKE);paint.setColor(ACCENT);paint.setStrokeWidth(dp(2));c.drawRoundRect(v.getX()-dp(3),v.getY()-dp(3),v.getX()+v.getWidth()+dp(3),v.getY()+v.getHeight()+dp(3),dp(16),dp(16),paint);paint.setStyle(Paint.Style.FILL);}
        }
    }
    private final class Pad extends View {
        int pointer=-1;
        Pad(Context c){super(c);setContentDescription("Direcional com diagonais");setFocusable(true);}
        @Override protected void onDraw(Canvas c){
            float s=getWidth()/3f,h=getHeight()/3f;paint.setColor(0x88202a35);c.drawRoundRect(0,h,getWidth(),h*2,dp(12),dp(12),paint);c.drawRoundRect(s,0,s*2,getHeight(),dp(12),dp(12),paint);
            paint.setColor(0xccf3f0e8);c.drawCircle(s*1.5f,h*1.5f,dp(4),paint);paint.setTextSize(dp(24));paint.setTextAlign(Paint.Align.CENTER);paint.setTypeface(regular);
            int[] bits={64,128,32,16};String[] glyph={"↑","↓","←","→"};float[] xs={s*1.5f,s*1.5f,s*.5f,s*2.5f},ys={h*.5f,h*2.5f,h*1.5f,h*1.5f};
            for(int i=0;i<4;i++){paint.setColor((padKeys&bits[i])!=0?ACCENT:INK);c.drawText(glyph[i],xs[i],ys[i]+dp(8),paint);}
        }
        @Override public boolean onTouchEvent(MotionEvent e){
            if(editing)return editTouch("D-pad",this,e);int action=e.getActionMasked();
            if(action==MotionEvent.ACTION_DOWN){pointer=e.getPointerId(0);haptic(this);}
            if(action==MotionEvent.ACTION_UP||action==MotionEvent.ACTION_CANCEL||(action==MotionEvent.ACTION_POINTER_UP&&e.getPointerId(e.getActionIndex())==pointer)){pointer=-1;padKeys=0;}
            else{int i=e.findPointerIndex(pointer);if(i>=0){float x=(e.getX(i)-getWidth()/2f)/(getWidth()/2f),y=(e.getY(i)-getHeight()/2f)/(getHeight()/2f);padKeys=0;if(Math.max(Math.abs(x),Math.abs(y))>.18f){if(Math.abs(x)>.38f*Math.abs(y))padKeys|=x<0?32:16;if(Math.abs(y)>.38f*Math.abs(x))padKeys|=y<0?64:128;}}}
            engine.input.touch(buttonKeys|padKeys);invalidate();return true;
        }
    }
}
