#include <android/NeuralNetworks.h>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <chrono>
#include <sys/socket.h>
#include <sys/un.h>
#include <sys/types.h>
#include <sys/wait.h>
#include <unistd.h>
#include <signal.h>
#include <cerrno>
#include <cstdlib>
static int run_nnapi(int argc,char**argv){
 uint32_t n=0;int rc=ANeuralNetworks_getDeviceCount(&n);printf("device_count rc=%d n=%u\n",rc,n);
 ANeuralNetworksDevice* selected=nullptr;
 for(uint32_t i=0;i<n;i++){ANeuralNetworksDevice* d=nullptr;const char* name="?";int32_t type=0;if(ANeuralNetworks_getDevice(i,&d)==0){ANeuralNetworksDevice_getName(d,&name);ANeuralNetworksDevice_getType(d,&type);printf("%u: %s type=%d\n",i,name?name:"?",type);if(name&&strstr(name,"qti-dsp"))selected=d;}}
 if(argc<2||strcmp(argv[1],"test"))return 0;
 if(!selected){puts("qti-dsp not exposed via NNAPI NDK");return 10;}
 ANeuralNetworksModel* m=nullptr;ANeuralNetworksCompilation* c=nullptr;ANeuralNetworksExecution* e=nullptr;
 if((rc=ANeuralNetworksModel_create(&m)))goto done;
 {uint32_t dims[]={1,4};ANeuralNetworksOperandType tensor={ANEURALNETWORKS_TENSOR_FLOAT32,2,dims,0,0};ANeuralNetworksOperandType scalar={ANEURALNETWORKS_INT32,0,nullptr,0,0};for(int i=0;i<3;i++)if((rc=ANeuralNetworksModel_addOperand(m,&tensor)))goto done;if((rc=ANeuralNetworksModel_addOperand(m,&scalar)))goto done;int32_t activation=0;if((rc=ANeuralNetworksModel_setOperandValue(m,3,&activation,sizeof activation)))goto done;uint32_t ins[]={0,1,3},out[]={2};if((rc=ANeuralNetworksModel_addOperation(m,ANEURALNETWORKS_ADD,3,ins,1,out)))goto done;uint32_t modelins[]={0,1};if((rc=ANeuralNetworksModel_identifyInputsAndOutputs(m,2,modelins,1,out)))goto done;}
 if((rc=ANeuralNetworksModel_finish(m)))goto done;
 {bool supported=false;if((rc=ANeuralNetworksModel_getSupportedOperationsForDevices(m,(const ANeuralNetworksDevice* const*)&selected,1,&supported)))goto done;if(!supported){rc=11;puts("ADD unsupported by qti-dsp");goto done;}}
 if((rc=ANeuralNetworksCompilation_createForDevices(m,(const ANeuralNetworksDevice* const*)&selected,1,&c)))goto done;
 ANeuralNetworksCompilation_setPreference(c,ANEURALNETWORKS_PREFER_LOW_POWER);
 if((rc=ANeuralNetworksCompilation_finish(c)))goto done;
 if((rc=ANeuralNetworksExecution_create(c,&e)))goto done;
 {float a[]={1,2,3,4},b[]={5,6,7,8},out[4]={};if((rc=ANeuralNetworksExecution_setInput(e,0,nullptr,a,sizeof a)))goto done;if((rc=ANeuralNetworksExecution_setInput(e,1,nullptr,b,sizeof b)))goto done;if((rc=ANeuralNetworksExecution_setOutput(e,0,nullptr,out,sizeof out)))goto done;auto start=std::chrono::steady_clock::now();rc=ANeuralNetworksExecution_compute(e);auto us=std::chrono::duration_cast<std::chrono::microseconds>(std::chrono::steady_clock::now()-start).count();printf("qti-dsp compute rc=%d latency_us=%lld\n",rc,(long long)us);if(rc==0)for(int i=0;i<4;i++)if(std::fabs(out[i]-(a[i]+b[i]))>0.01f)rc=12;}
 done:printf("final rc=%d\n",rc);if(e)ANeuralNetworksExecution_free(e);if(c)ANeuralNetworksCompilation_free(c);if(m)ANeuralNetworksModel_free(m);return rc;
}

static constexpr const char* SOCKET_NAME="poxel_neuralbridge_v1";
static void socket_addr(sockaddr_un& addr){
 memset(&addr,0,sizeof(addr));addr.sun_family=AF_UNIX;
 strncpy(addr.sun_path+1,SOCKET_NAME,sizeof(addr.sun_path)-2);
}
static int daemon_main(){
 signal(SIGPIPE,SIG_IGN);
 int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);
 if(fd<0){perror("socket");return 20;}
 sockaddr_un addr;socket_addr(addr);
 if(bind(fd,(sockaddr*)&addr,sizeof(addr))<0){perror("bind");close(fd);return 21;}
 if(listen(fd,4)<0){perror("listen");close(fd);return 22;}
 printf("NeuralBridge IPC listening (root-only)\n");fflush(stdout);
 for(;;){
  int client=accept(fd,nullptr,nullptr);if(client<0){if(errno==EINTR)continue;break;}
  struct ucred peer{};socklen_t len=sizeof(peer);
  if(getsockopt(client,SOL_SOCKET,SO_PEERCRED,&peer,&len)!=0||peer.uid!=0){
   const char* msg="Access denied: root required\n";write(client,msg,strlen(msg));close(client);continue;
  }
  char cmd[32]={};ssize_t n=read(client,cmd,sizeof(cmd)-1);
  if(n<=0){close(client);continue;}
  const char* sub=nullptr;
  if(!strncmp(cmd,"list",4))sub="list";
  else if(!strncmp(cmd,"test",4))sub="test";
  else {const char* msg="Commands: list | test\n";write(client,msg,strlen(msg));close(client);continue;}
  pid_t pid=fork();
  if(pid==0){
   close(fd);dup2(client,STDOUT_FILENO);dup2(client,STDERR_FILENO);close(client);
   char a0[]="neuralbridge";char a1[8];strncpy(a1,sub,sizeof(a1));a1[sizeof(a1)-1]=0;
   char* args[]={a0,a1,nullptr};int rc=run_nnapi(2,args);fflush(stdout);fflush(stderr);_exit(rc);
  }
  if(pid>0){close(client);int status=0;waitpid(pid,&status,0);}
  else {perror("fork");close(client);}
 }
 close(fd);return 23;
}
static int client_main(const char* command){
 int fd=socket(AF_UNIX,SOCK_STREAM|SOCK_CLOEXEC,0);if(fd<0)return 30;
 sockaddr_un addr;socket_addr(addr);
 if(connect(fd,(sockaddr*)&addr,sizeof(addr))<0){perror("connect");close(fd);return 31;}
 write(fd,command,strlen(command));shutdown(fd,SHUT_WR);
 char buf[1024];ssize_t n;while((n=read(fd,buf,sizeof(buf)))>0)write(STDOUT_FILENO,buf,n);
 close(fd);return 0;
}
int main(int argc,char**argv){
 setvbuf(stdout,nullptr,_IONBF,0);
 if(argc>=2&&!strcmp(argv[1],"daemon"))return daemon_main();
 if(argc>=3&&!strcmp(argv[1],"client"))return client_main(argv[2]);
 return run_nnapi(argc,argv);
}
