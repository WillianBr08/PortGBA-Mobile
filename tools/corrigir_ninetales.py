#!/usr/bin/env python3
"""Corrige somente a ROM Elite Redux 2.65.4.1b conhecida; preserva backup e saves."""
import argparse,hashlib,json,os,shutil,tempfile
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
ORIGINAL='926098fa9f6b29b469fa351e80e4f018285006ad10a4eb5bc255d14d6d54acfc'
def fix(path):
 path=Path(path).expanduser().resolve();data=bytearray(path.read_bytes());before=hashlib.sha256(data).hexdigest()
 entry=bytes.fromhex('c046004b184701f0ff09');payload=(ROOT/'docs/compatibility/flame-payload.bin').read_bytes()
 if data[0x9db66:0x9db70]==entry and data[0x1fff000:0x1fff000+len(payload)]==payload:
  print('Esta ROM já contém a correção.');return
 if before!=ORIGINAL:raise ValueError('ROM diferente da versão comprovada. Nenhum arquivo foi alterado.')
 backup=path.with_name(path.name+'.antes-ninetales.bak')
 if backup.exists():
  if hashlib.sha256(backup.read_bytes()).hexdigest()!=before:raise ValueError('Já existe um backup diferente; preservado sem alterações.')
 else:shutil.copy2(path,backup)
 data[0x9db66:0x9db70]=entry;data[0x1fff000:0x1fff000+len(payload)]=payload
 fd,tmp=tempfile.mkstemp(prefix=path.name+'.',suffix='.tmp',dir=path.parent)
 try:
  with os.fdopen(fd,'wb') as f:f.write(data);f.flush();os.fsync(f.fileno())
  shutil.copymode(path,tmp);os.replace(tmp,path)
 finally:
  if os.path.exists(tmp):os.unlink(tmp)
 report={'rom':str(path),'backup':str(backup),'before_sha256':before,'after_sha256':hashlib.sha256(data).hexdigest(),'modified_ranges':[[0x9db66,len(entry)],[0x1fff000,len(payload)]],'saves_modified':False}
 (ROOT/'docs/compatibility/pc-patch.json').write_text(json.dumps(report,indent=2)+'\n')
 print('ROM corrigida:',path);print('Original preservada:',backup);print('Feche e reabra a ROM no emulador para ativar a correção.')
if __name__=='__main__':
 p=argparse.ArgumentParser(description=__doc__);p.add_argument('rom');a=p.parse_args()
 try:fix(a.rom)
 except Exception as ex:p.exit(1,str(ex)+'\n')
