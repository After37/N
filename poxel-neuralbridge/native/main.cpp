#include <android/NeuralNetworks.h>
#include <cstdio>
#include <cstring>
#include <cmath>
#include <chrono>
int main(int argc,char**argv){
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
