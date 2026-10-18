#include "engine.h"
#include <mgba/core/serialize.h>
#include <mgba-util/vfs.h>
#include <stdio.h>
#include <stdlib.h>
#include <fcntl.h>
#include <time.h>
#include <unistd.h>
#include <sys/stat.h>
#define CHECK(x) do{if(!(x)){fprintf(stderr,"FAIL: %s\n",#x);exit(1);}}while(0)
static double now(void){struct timespec t;clock_gettime(CLOCK_MONOTONIC,&t);return t.tv_sec*1000.+t.tv_nsec/1e6;}
static double cpu(void){struct timespec t;clock_gettime(CLOCK_THREAD_CPUTIME_ID,&t);return t.tv_sec*1000.+t.tv_nsec/1e6;}
static int cmp(const void*a,const void*b){double x=*(double*)a,y=*(double*)b;return(x>y)-(x<y);}
static unsigned digest(struct EliteEngine* e){unsigned h=2166136261u;for(int i=0;i<240*160;i++)h=(h^e->pixels[i])*16777619u;return h;}
static int save_format(struct EliteEngine*e,const char*path,int screenshot){
 char tmp[4096];snprintf(tmp,sizeof(tmp),"%s.tmp",path);struct VFile*vf=VFileOpen(tmp,O_CREAT|O_TRUNC|O_RDWR);if(!vf)return 0;
 int ok=mCoreSaveStateNamed(e->core,vf,SAVESTATE_SAVEDATA|SAVESTATE_RTC|SAVESTATE_METADATA|(screenshot?SAVESTATE_SCREENSHOT:0));
 ok=vf->sync(vf,NULL,0)&&ok;vf->close(vf);if(ok)ok=rename(tmp,path)==0;return ok;
}
int main(int argc,char**argv){
 CHECK(argc==4||argc==5);mkdir(argv[2],0700);char battery[4096],path[4096];snprintf(battery,sizeof(battery),"%s/bench.sav",argv[2]);
 struct EliteEngine*e=elite_open(argv[1],battery);CHECK(e);short audio[4096];for(int i=0;i<120;i++)elite_step(e,0,1,audio,4096);
 if(argv[3][0]!='-')CHECK(elite_state(e,argv[3],0));
 char initial[4096];snprintf(initial,sizeof(initial),"%s/initial.ss",argv[2]);CHECK(elite_state(e,initial,1));
 double median[3],cpuMedian[3];const char* names[]={"PNG compatible","raw compatible","app writer"};
 for(int mode=0;mode<3;mode++){
  CHECK(elite_state(e,initial,0));elite_step(e,0,1,audio,4096);mCoreConfigSetIntValue(&e->core->config,"stateCompressionLevel",mode==0?6:1);
  snprintf(path,sizeof(path),"%s/mode%d.ss",argv[2],mode);double times[25],work[25];
  for(int i=0;i<25;i++){double begin=now(),workBegin=cpu();CHECK(mode==2?elite_state(e,path,1):save_format(e,path,mode==0));times[i]=now()-begin;work[i]=cpu()-workBegin;}
  qsort(times,25,sizeof(double),cmp);median[mode]=times[12];qsort(work,25,sizeof(double),cmp);cpuMedian[mode]=work[12];struct stat st;CHECK(stat(path,&st)==0);
  CHECK(elite_state(e,path,0));elite_step(e,1,1,audio,4096);unsigned expected=digest(e);
  CHECK(elite_state(e,path,0));elite_step(e,1,1,audio,4096);CHECK(expected==digest(e));
  printf("%s: median=%.3f ms p95=%.3f ms CPU=%.3f ms bytes=%ld deterministic=PASS\n",names[mode],median[mode],times[23],cpuMedian[mode],(long)st.st_size);
 }
 CHECK(elite_flush(e));elite_close(e);
 if(argc==5&&cpuMedian[2]>cpuMedian[0]*.65){fputs("FAIL: app writer has not reduced serialization CPU by at least 35% versus PNG\n",stderr);return 2;}
 puts("PASS: all state formats roundtrip with battery data and RTC");return 0;
}
