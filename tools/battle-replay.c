#include "engine.h"
#include <mgba/internal/arm/arm.h>
#include <stdio.h>
#include <stdlib.h>
#include <sys/stat.h>
#include <signal.h>
#include <unistd.h>
static volatile sig_atomic_t frame;
static void timeout_frame(int sig) { (void)sig; char b[128];int n=snprintf(b,sizeof(b),"FAIL: emulated frame %d never returned\n",frame);write(2,b,n);_exit(2); }
static void image(struct EliteEngine* e,const char* dir,int n) {
 char p[4096];snprintf(p,sizeof(p),"%s/%05d.ppm",dir,n);FILE* f=fopen(p,"wb");if(!f)exit(1);fprintf(f,"P6\n240 160\n255\n");
 for(int i=0;i<240*160;i++){unsigned c=e->pixels[i];unsigned char rgb[]={c&255,(c>>8)&255,(c>>16)&255};fwrite(rgb,1,3,f);}fclose(f);
}
int main(int argc,char** argv) {
 if(argc<4){fprintf(stderr,"usage: battle-replay ROM STATE OUTPUT [frames] [keys=1] [hold=4] [period=45]\n");return 1;}
 mkdir(argv[3],0777);char battery[4096];snprintf(battery,sizeof(battery),"%s/replay.sav",argv[3]);
 struct EliteEngine* e=elite_open(argv[1],battery);if(!e||!elite_state(e,argv[2],0)){fputs("cannot load state\n",stderr);return 1;}
 int total=argc>4?atoi(argv[4]):3600,keys=argc>5?atoi(argv[5]):1,hold=argc>6?atoi(argv[6]):4,period=argc>7?atoi(argv[7]):45;
 signal(SIGALRM,timeout_frame);short audio[4096];unsigned last=0;int unchanged=0;
 for(frame=0;frame<total;frame++){
  alarm(3);elite_step(e,frame%period<hold?keys:0,1,audio,4096);alarm(0);
  unsigned hash=2166136261u;for(int i=0;i<240*160;i++)hash=(hash^e->pixels[i])*16777619u;
  unchanged=hash==last?unchanged+1:0;last=hash;
  if(frame%60==0){image(e,argv[3],frame);printf("frame=%d pc=%08x hash=%08x still=%d\n",frame,((struct ARMCore*)e->core->cpu)->gprs[15],hash,unchanged);fflush(stdout);}
  if(unchanged==900){
   image(e,argv[3],frame);char p[4096];snprintf(p,sizeof(p),"%s/stuck.ss",argv[3]);elite_state(e,p,1);
   for(int b=2;b<=3;b++){snprintf(p,sizeof(p),"%s/ram%d.bin",argv[3],b);FILE* f=fopen(p,"wb");int length=b==2?0x40000:0x8000;for(int off=0;off<length;off++){unsigned char v=e->core->busRead8(e->core,(b<<24)+off);fwrite(&v,1,1,f);}fclose(f);}
   fprintf(stderr,"FAIL: framebuffer unchanged for 900 frames at frame=%d pc=%08x\n",frame,((struct ARMCore*)e->core->cpu)->gprs[15]);elite_close(e);return 3;
  }
 }
 image(e,argv[3],total);elite_close(e);puts("Completed replay without frame hang; inspect battle progression images.");return 0;
}
