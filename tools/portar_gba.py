#!/usr/bin/env python3
"""GBA Studio: cria APKs locais com a mesma interface. Sem modificar a ROM de entrada."""
import argparse
import hashlib
import json
import os
from pathlib import Path
import re
import shutil
import subprocess
import sys
import tempfile
import threading
import unicodedata
import xml.etree.ElementTree as ET
from zipfile import ZipFile, ZIP_STORED, ZIP_DEFLATED

ROOT = Path(__file__).resolve().parents[1]
ANDROID = '{http://schemas.android.com/apk/res/android}'
ET.register_namespace('android', ANDROID[1:-1])

def slug(text):
    text = unicodedata.normalize('NFKD', text).encode('ascii', 'ignore').decode()
    return re.sub(r'[^A-Za-z0-9._-]+', '-', text).strip('-._')[:80] or 'GBA'

def inspect_rom(path):
    path = Path(path).expanduser().resolve()
    if not path.is_file(): raise ValueError('Escolha um arquivo .gba existente.')
    if path.suffix.lower() != '.gba' or not 192 <= path.stat().st_size <= 50_000_000:
        raise ValueError('Use uma ROM GBA descompactada de até 50 MB.')
    data = path.read_bytes()
    if data[0xB2] != 0x96: raise ValueError('O arquivo não tem um cabeçalho GBA reconhecível.')
    return path, data, hashlib.sha256(data).hexdigest()

def run(command, env, log):
    log('> ' + Path(str(command[0])).name + ' ' + ' '.join(str(x) for x in command[1:3]))
    proc = subprocess.Popen([str(x) for x in command], env=env, cwd=ROOT,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT, text=True)
    for line in proc.stdout: log(line.rstrip())
    if proc.wait(): raise RuntimeError('Falha na etapa acima. Veja a mensagem do comando.')

def make_port(rom, title=None, output=None, save=None, states=None, package=None, include_sidecars=False, log=print, logo="auto"):
    rom, data, digest = inspect_rom(rom)
    if logo=="auto":
        code=data[0xAC:0xB0].decode('ascii',errors='ignore')
        logo='emerald' if code.startswith('BPE') else 'firered' if code.startswith('BPR') else 'gba'
    if logo not in ('emerald','firered','gba'):raise ValueError('Logo inválida: escolha emerald, firered ou gba.')
    title = (title or rom.stem).strip()
    if not title or len(title) > 100: raise ValueError('Use um nome entre 1 e 100 caracteres.')
    package = package or 'dev.privado.gba.g' + digest[:16]
    if not re.fullmatch(r'[a-z][a-z0-9_]*(?:\.[a-z][a-z0-9_]*){2,}', package):
        raise ValueError('Identificador inválido. Exemplo: dev.privado.meujogo')
    if package == 'dev.privado.eliteredux': raise ValueError('Este identificador está reservado ao seu Elite Redux.')
    destination = Path(output or ROOT / 'ports' / slug(title)).expanduser().resolve()
    destination.mkdir(parents=True, exist_ok=True)
    apk_path = destination / (slug(title) + '.apk')
    if apk_path.exists(): raise FileExistsError('O APK já existe. Escolha outra pasta ou remova a cópia anterior após guardá-la.')
    sdk = Path(os.environ.get('ANDROID_SDK_ROOT', os.environ.get('ANDROID_HOME', str(Path.home()/'Android/Sdk'))))
    jdk = Path(os.environ.get('ELITE_JAVA_HOME', os.environ.get('JAVA_HOME', '/usr/lib/jvm/java-17-openjdk')))
    bt = sdk / 'build-tools/35.0.0'
    android_jar = sdk / 'platforms/android-36/android.jar'
    for p in [bt/'aapt2', bt/'zipalign', bt/'apksigner', android_jar, jdk/'bin/java']:
        if not p.is_file(): raise FileNotFoundError('Dependência ausente: '+str(p))
    env = dict(os.environ, JAVA_HOME=str(jdk), PATH=str(jdk/'bin')+os.pathsep+os.environ.get('PATH',''))
    cache = ROOT/'build'
    # Incremental native compilation ensures ports always use the current interface.
    log('Verificando e compilando a interface atual…')
    run([ROOT/'build.sh','--template-only'], env, log)
    libraries = sorted((cache/'apk/lib').glob('*/*.so'))
    if not libraries: raise RuntimeError('O núcleo nativo não foi compilado.')
    base = rom.stem
    if '/' in base or '\\' in base or base in ('.','..'): raise ValueError('Nome de ROM inválido.')
    seeds = {}
    if include_sidecars:
        candidate = rom.with_suffix('.sav')
        if candidate.is_file(): seeds[base+'.sav'] = candidate
        for n in range(1,10):
            candidate=rom.with_suffix('.ss'+str(n))
            if candidate.is_file(): seeds[base+'.ss'+str(n)] = candidate
    if save: seeds[base+'.sav'] = Path(save).expanduser().resolve()
    used=set()
    for index, state in enumerate(states or [],1):
        p=Path(state).expanduser().resolve();m=re.search(r'\.ss([1-9])$',p.name,re.I);slot=int(m.group(1)) if m else index
        if slot > 9 or slot in used: raise ValueError('Escolha no máximo nove estados, sem números de slot repetidos.')
        used.add(slot);seeds[base+'.ss'+str(slot)]=p
    for name,p in seeds.items():
        if not p.is_file(): raise FileNotFoundError('Save não encontrado: '+str(p))
        if name.endswith('.sav') and p.stat().st_size not in (512,8192,32768,65536,131072,131088):
            raise ValueError('Tamanho de .sav incompatível: '+str(p))
        if not name.endswith('.sav') and not 32 <= p.stat().st_size <= 4*1024*1024:
            raise ValueError('O estado parece inválido: '+str(p))
    log('Jogo: '+title+' | '+str(len(data)//1024)+' KiB | '+str(len(seeds))+' saves/estados')
    log('Aplicativo independente: '+package)
    with tempfile.TemporaryDirectory(prefix='gba-port-',dir=destination) as tmp_name:
        tmp=Path(tmp_name)
        tree=ET.parse(ROOT/'android/app/src/main/AndroidManifest.xml');manifest=tree.getroot();manifest.set('package',package)
        app=manifest.find('application');app.set(ANDROID+'label',title);app.set(ANDROID+'icon','@drawable/icon_'+logo)
        manifest_path=tmp/'AndroidManifest.xml';tree.write(manifest_path,encoding='utf-8',xml_declaration=True)
        unsigned=tmp/'unsigned.apk'
        run([bt/'aapt2','link','-I',android_jar,'--manifest',manifest_path,'--auto-add-overlay','-o',unsigned,cache/'res/resources.zip'],env,log)
        with ZipFile(unsigned,'a') as apk:
            apk.write(cache/'dex/classes.dex','classes.dex',compress_type=ZIP_DEFLATED)
            for lib in libraries: apk.write(lib,'lib/'+str(lib.relative_to(cache/'apk/lib')),compress_type=ZIP_STORED)
            for folder in ['fonts','licenses']:
                for p in sorted((ROOT/'android/app/src/main/assets'/folder).rglob('*')):
                    if p.is_file():apk.write(p,'assets/'+str(p.relative_to(ROOT/'android/app/src/main/assets')),compress_type=ZIP_DEFLATED)
            spec={'title':title,'base':base,'folder':title.replace('/','_').replace('\\','_'),
                  'description':'Game Boy Advance','rom_sha256':digest,'icon':logo}
            apk.writestr('assets/game.json',json.dumps(spec,ensure_ascii=False),compress_type=ZIP_DEFLATED)
            apk.writestr('assets/game/'+base+'.gba',data,compress_type=ZIP_DEFLATED)
            for name,p in seeds.items():apk.write(p,'assets/game/'+name,compress_type=ZIP_DEFLATED)
        aligned=tmp/'aligned.apk'
        run([bt/'zipalign','-f','-P','16','4',unsigned,aligned],env,log)
        keystore=ROOT/'.private/elite-redux.jks'
        if not keystore.is_file():
            keystore.parent.mkdir(exist_ok=True)
            run([jdk/'bin/keytool','-genkeypair','-keystore',keystore,'-storepass','elite-redux-local','-keypass','elite-redux-local','-alias','elite-redux','-keyalg','RSA','-keysize','3072','-validity','36500','-dname','CN=GBA Studio Local, O=Private, C=BR'],env,log)
            keystore.chmod(0o600)
        signed=tmp/'signed.apk'
        run([bt/'apksigner','sign','--ks',keystore,'--ks-pass','pass:elite-redux-local','--key-pass','pass:elite-redux-local','--out',signed,aligned],env,log)
        run([bt/'apksigner','verify','--verbose',signed],env,log)
        with ZipFile(signed) as apk:
            game_files=[n for n in apk.namelist() if n.startswith('assets/game/')]
            if len([n for n in game_files if n.endswith('.gba')])!=1:raise RuntimeError('Empacotamento de ROM inesperado.')
            if hashlib.sha256(apk.read('assets/game/'+base+'.gba')).hexdigest()!=digest:raise RuntimeError('A ROM mudou durante o empacotamento.')
        os.replace(signed,apk_path)
    if hashlib.sha256(rom.read_bytes()).hexdigest()!=digest:raise RuntimeError('A ROM de origem mudou durante a execução.')
    apk_digest=hashlib.sha256(apk_path.read_bytes()).hexdigest()
    (destination/(apk_path.name+'.sha256')).write_text(apk_digest+'  '+apk_path.name+'\n')
    report={'title':title,'package':package,'rom_sha256':digest,'apk_sha256':apk_digest,'included_saves':list(seeds),'apk':apk_path.name,'interface_version':'4.0','logo':logo}
    (destination/'port.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n')
    (destination/'LEIA-ME.txt').write_text('Instale '+apk_path.name+' no Android 8 ou posterior.\nO jogo abre diretamente. Menu tem Jogo, Saves e Ajustes; Paisagem fica em Jogo.\nO APK inclui sua ROM e os saves selecionados. Os arquivos originais não foram alterados.\nExporte seus saves pelo menu antes de desinstalar.\nCompatibilidade e desempenho dependem da ROM e do aparelho.\n',encoding='utf-8')
    log('Pronto: '+str(apk_path));return apk_path

def gui():
    import tkinter as tk
    from tkinter import ttk,filedialog,messagebox
    win=tk.Tk();win.title('GBA Studio · Criar aplicativo');win.geometry('780x780');win.minsize(640,560);win.configure(bg='#101820')
    style=ttk.Style();style.theme_use('clam');style.configure('TFrame',background='#101820');style.configure('TLabel',background='#101820',foreground='#f3f0e8',font=('Sans',11));style.configure('TButton',font=('Sans',11),padding=10);style.configure('TEntry',padding=9)
    footer=ttk.Frame(win,padding=(24,8,24,16));footer.pack(side='bottom',fill='x')
    viewport=ttk.Frame(win);viewport.pack(fill='both',expand=True)
    canvas=tk.Canvas(viewport,bg='#101820',highlightthickness=0);scroll=ttk.Scrollbar(viewport,orient='vertical',command=canvas.yview);canvas.configure(yscrollcommand=scroll.set);scroll.pack(side='right',fill='y');canvas.pack(side='left',fill='both',expand=True)
    panel=ttk.Frame(canvas,padding=24);window=canvas.create_window((0,0),window=panel,anchor='nw')
    panel.bind('<Configure>',lambda e:canvas.configure(scrollregion=canvas.bbox('all')))
    canvas.bind('<Configure>',lambda e:canvas.itemconfigure(window,width=e.width))
    win.bind('<MouseWheel>',lambda e:canvas.yview_scroll(-1 if e.delta>0 else 1,'units'))
    win.bind('<Button-4>',lambda e:canvas.yview_scroll(-1,'units'));win.bind('<Button-5>',lambda e:canvas.yview_scroll(1,'units'))
    ttk.Label(panel,text='Seu GBA. Seu aplicativo.',font=('Sans',25,'bold')).pack(anchor='w')
    ttk.Label(panel,text='Escolha a ROM. A interface, os controles e os saves vão junto.',wraplength=660).pack(anchor='w',pady=(8,24))
    fields={k:tk.StringVar() for k in ['rom','title','output','save']};state_files=[]
    def field(label,key,browse=None):
        ttk.Label(panel,text=label).pack(anchor='w',pady=(10,4));row=ttk.Frame(panel);row.pack(fill='x');ttk.Entry(row,textvariable=fields[key]).pack(side='left',fill='x',expand=True)
        if browse:ttk.Button(row,text='Escolher',command=browse).pack(side='right',padx=(8,0))
    def choose_rom():
        p=filedialog.askopenfilename(title='ROM GBA',filetypes=[('Game Boy Advance','*.gba *.GBA')])
        if p:fields['rom'].set(p);fields['title'].set(Path(p).stem);fields['output'].set(str(ROOT/'ports'/slug(Path(p).stem)))
    field('ROM do jogo','rom',choose_rom);field('Nome no celular','title')
    field('Pasta de saída','output',lambda:fields['output'].set(filedialog.askdirectory() or fields['output'].get()))
    field('Save .sav opcional','save',lambda:fields['save'].set(filedialog.askopenfilename(filetypes=[('Save','*.sav')]) or fields['save'].get()))
    ttk.Label(panel,text='Logo do aplicativo').pack(anchor='w',pady=(10,4))
    logo_options={'Automática · detectar ROM':'auto','Emerald · Rayquaza':'emerald','FireRed · Charizard':'firered','GBA · neutra':'gba'}
    logo_choice=tk.StringVar(value='Automática · detectar ROM')
    picker=ttk.Combobox(panel,textvariable=logo_choice,values=list(logo_options),state='readonly');picker.pack(fill='x')
    sidecars=tk.BooleanVar(value=False)
    ttk.Checkbutton(panel,text='Incluir saves que estão ao lado da ROM',variable=sidecars).pack(anchor='w',pady=(10,0))
    state_label=ttk.Label(panel,text='Nenhum save pessoal é incluído por padrão.');state_label.pack(anchor='w',pady=(12,0))
    def choose_states():
        found=filedialog.askopenfilenames(title='Save states mGBA',filetypes=[('Estados mGBA','*.ss* *.png'),('Todos','*')])
        if found:state_files[:]=found;state_label.configure(text=str(len(found))+' estados selecionados')
    ttk.Button(panel,text='Adicionar save states…',command=choose_states).pack(anchor='w',pady=8)
    button=ttk.Button(footer,text='Criar APK');button.pack(fill='x')
    logbox=tk.Text(panel,height=4,bg='#17212c',fg='#b9c8d6',insertbackground='white',relief='flat',font=('Monospace',9));logbox.pack(fill='both',expand=True,pady=(8,12));logbox.configure(state='disabled')
    def log(line):
        def write():logbox.configure(state='normal');logbox.insert('end',line+'\n');logbox.see('end');logbox.configure(state='disabled')
        win.after(0,write)
    def create():
        values={k:v.get().strip() for k,v in fields.items()};chosen_logo=logo_options[logo_choice.get()];include_sidecars=sidecars.get();button.configure(state='disabled')
        def work():
            try:
                apk=make_port(values['rom'],values['title'],values['output'],values['save'] or None,list(state_files),include_sidecars=include_sidecars,log=log,logo=chosen_logo)
                win.after(0,lambda:messagebox.showinfo('Aplicativo pronto',str(apk)))
            except Exception as ex:
                message=str(ex);log(message);win.after(0,lambda:messagebox.showerror('Não foi possível criar',message))
            finally:win.after(0,lambda:button.configure(state='normal'))
        threading.Thread(target=work,daemon=True).start()
    button.configure(command=create);win.mainloop()

def main():
    p=argparse.ArgumentParser(description=__doc__);p.add_argument('rom',nargs='?');p.add_argument('--title');p.add_argument('--output');p.add_argument('--save');p.add_argument('--state',action='append');p.add_argument('--package');p.add_argument('--sem-saves',action='store_true');p.add_argument('--gui',action='store_true');p.add_argument('--logo',choices=['auto','emerald','firered','gba'],default='auto');p.add_argument('--include-saves',action='store_true')
    args=p.parse_args()
    if args.gui or not args.rom:gui();return
    try:make_port(args.rom,args.title,args.output,args.save,args.state,args.package,args.include_saves and not args.sem_saves,logo=args.logo)
    except Exception as ex:p.exit(1,'Erro: '+str(ex)+'\n')
if __name__=='__main__':main()
