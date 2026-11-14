#include "guard_core.h"
#include <cstdint>
#include <cstdio>
#include <cstring>
#include <sys/mman.h>
#include <unistd.h>
#include <vector>
#include <string>
static std::string hex(const std::uint8_t* d){char b[65];for(int i=0;i<32;i++)std::snprintf(b+i*2,3,"%02x",d[i]);return b;}
int main(int argc,char**argv){
  if(argc>=2&&!std::strcmp(argv[1],"sha")){ // sha <file>
    FILE*f=fopen(argv[2],"rb");std::vector<std::uint8_t> v;int c;while((c=fgetc(f))!=EOF)v.push_back(c);fclose(f);
    std::uint8_t o[32];guard::sha256(v.data(),v.size(),o);puts(hex(o).c_str());return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"unmask")){ // unmask <maskedHex> <maskHex>
    std::uint8_t mask[32];
    const bool maskOk=guard::parseHex32(argv[3],mask);
    char out[65];
    const bool ok=maskOk&&guard::unmaskFingerprintHex(argv[2],mask,out);
    std::printf("%d %s\n",ok?1:0,ok?out:"");return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"textcheck")){ // textcheck
    const int before=(int)guard::verifyTextIntegrity();
    std::uintptr_t s=0,e=0;
    const bool located=guard::locateOwnTextSegment(reinterpret_cast<std::uintptr_t>(&guard::verifyTextIntegrity),s,e);
    int after=-1;
    if(located&&e>s){
      const std::uintptr_t target=s+1;
      const long pageSize=sysconf(_SC_PAGESIZE);
      const std::uintptr_t pageStart=target&~(static_cast<std::uintptr_t>(pageSize)-1);
      if(mprotect(reinterpret_cast<void*>(pageStart),static_cast<std::size_t>(pageSize),PROT_READ|PROT_WRITE|PROT_EXEC)==0){
        unsigned char*p=reinterpret_cast<unsigned char*>(target);
        *p=static_cast<unsigned char>(*p^0xFF);
        after=(int)guard::verifyTextIntegrity();
      }
    }
    std::printf("%d %d %d\n",before,located?1:0,after);return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"tracer")){ // tracer  (expect 0=none when run un-traced; re-run under strace/gdb to see 1)
    std::printf("%d\n",(int)guard::checkTracerPid());return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"root")){ // root
    std::printf("%d\n",(int)guard::checkRootIndicators());return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"fridaport")){ // fridaport  (run a listener on 127.0.0.1:27042 to see 1)
    std::printf("%d\n",(int)guard::checkFridaDefaultPort());return 0;}
  if(argc>=2&&!std::strcmp(argv[1],"fp")){ // fp <apk> <expectedHex>
    std::uint8_t fp[32];std::memset(fp,0xAA,32);
    const int r=(int)guard::verifyApk(argv[2], argc>3?argv[3]:"", fp);
    std::printf("%d %s\n",r,hex(fp).c_str());return 0;}
  std::printf("%d\n",(int)guard::verifyApk(argv[2], argc>3?argv[3]:""));
}
