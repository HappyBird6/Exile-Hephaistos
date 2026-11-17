package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.*;
import java.nio.file.*;
import java.time.Clock;
import java.lang.management.*;
import java.util.*;
import org.junit.jupiter.api.Test;

public class DurableSolarContinuationProbe {
  static List<CertifiedMethodProtocol.Method> methods(MethodSearchService service,CertifiedMethodProtocol.Snapshot snapshot) {
    var rows=new ArrayList<CertifiedMethodProtocol.Method>(); int offset=0;
    while(true) { var page=service.certifiedGraph("probe",snapshot.jobId(),snapshot.revision(),0,offset);
      rows.addAll(page.methods()); if(page.nextMethodOffset()==null) return rows; offset=page.nextMethodOffset(); }
  }
  static Map<String,Object> memory() throws Exception {
    return Map.of("heapUsed",ManagementFactory.getMemoryMXBean().getHeapMemoryUsage().getUsed(),
        "heapPoolPeakSum",ManagementFactory.getMemoryPoolMXBeans().stream().filter(p->p.getType()==MemoryType.HEAP).mapToLong(p->p.getPeakUsage().getUsed()).sum(),
        "processStatus",Files.readAllLines(Path.of("/proc/self/status")).stream().filter(s->s.startsWith("VmHWM:")||s.startsWith("VmRSS:")).toList());
  }
  public static void main(String[] args) throws Exception {
    WorkbenchDefinitionsLoader.initialize(); var fixture=new PathSearchServiceTest();
    long start=System.nanoTime();
    try(var service=new MethodSearchService(fixture.catalog,fixture.goals,fixture.identity,fixture.json,Clock.systemUTC(),fixture.executor,1000000)) {
      service.persistence(new LocalCertifiedJobRepository(Path.of(args[0]),fixture.json));
      long recovered=System.nanoTime(); var snapshot=service.getCertified("probe",args[1]);
      Files.writeString(Path.of(args[2]),fixture.json.writeValueAsString(Map.of(
          "recoveryMs",(recovered-start)/1000000,"snapshot",snapshot,"methods",methods(service,snapshot),"memory",memory())));
    }
  }
  @Test void actualSolarJobCheckpointAndFreshProcessRecovery() throws Exception {
    var production=new DeferredProductionTest(); var fixture=production.fixture;
    var out=Path.of("/task-cache/bucket-method-verification/durable-solar-continuation"); Files.createDirectories(out);
    var dir=out.resolve("jobs");
    var request=new CertifiedMethodProtocol.Create(1,new MethodSearchProtocol.Create(2,"solar-durable-1000000",
        production.root(),production.goal(47),List.of()),new CertifiedInterval.Precision(64,2));
    var result=new LinkedHashMap<String,Object>();
    ManagementFactory.getMemoryPoolMXBeans().forEach(MemoryPoolMXBean::resetPeakUsage);
    String id;
    List<CertifiedMethodProtocol.Method> before;
    try(var service=new MethodSearchService(fixture.catalog,fixture.goals,fixture.identity,fixture.json,Clock.systemUTC(),fixture.executor,1000000)) {
      service.persistence(new LocalCertifiedJobRepository(dir,fixture.json));
      long start=System.nanoTime(); var created=service.createCertified("probe",request); id=created.search().jobId();
      result.put("firstResponseMs",(System.nanoTime()-start)/1000000);
      Long firstLocal=null; var segments=new ArrayList<Object>(); for(int segment=0;segment<16;segment++) {
      var failure=new java.util.concurrent.atomic.AtomicReference<Throwable>();
      var task=fixture.queue.remove(); var worker=new Thread(()->{try {task.run();}catch(Throwable error){failure.set(error);}},"solar-probe-worker");
      worker.start();
      while(worker.isAlive()) {
        var snapshot=service.getCertified("probe",id);
        if(firstLocal==null && snapshot.localRecommendations().stream().anyMatch(r->!r.methodIds().isEmpty())) firstLocal=(System.nanoTime()-start)/1000000;
        Thread.sleep(50);
      }
      worker.join(); if(failure.get()!=null) throw new AssertionError(failure.get());
      var checkpoint=service.getCertified("probe",id);
      segments.add(Map.of("segment",segment,"elapsedMs",(System.nanoTime()-start)/1000000,"snapshot",checkpoint));
      Files.writeString(out.resolve("segments.json"),fixture.json.writeValueAsString(segments));
      if(firstLocal!=null || !"COMPUTE_BUDGET".equals(checkpoint.reason()) || segment==15) break;
      service.mutateCertified("probe",id,new MethodSearchProtocol.Mutation(2,"continue-"+segment,checkpoint.revision(),"RESUME",null));
      }
      var snapshot=service.getCertified("probe",id); before=methods(service,snapshot);
      assertThat(snapshot.status()).isEqualTo("PAUSED"); assertThat(snapshot.enumerationComplete()).isFalse();
      result.put("runMs",(System.nanoTime()-start)/1000000); result.put("firstLocalObservedMs",firstLocal);
      result.put("wholeSearchComplete",false); result.put("snapshot",snapshot); result.put("memory",memory());
      result.put("request",request);
      var schedulerField=MethodSearchService.class.getDeclaredField("certified"); schedulerField.setAccessible(true);
      var scheduler=schedulerField.get(service); var jobsField=scheduler.getClass().getDeclaredField("jobs"); jobsField.setAccessible(true);
      var job=((Map<?,?>)jobsField.get(scheduler)).get(id); var engineField=job.getClass().getDeclaredField("engine"); engineField.setAccessible(true);
      var engine=(CertifiedMethodSearchEngine)engineField.get(job);
      result.put("retention",engine.retention()); result.put("workload",engine.workload());
    }
    Files.writeString(out.resolve("prefix.json"),fixture.json.writeValueAsString(result));
    var paths=new LinkedHashSet<String>();
    for(ClassLoader loader=getClass().getClassLoader();loader!=null;loader=loader.getParent())
      if(loader instanceof java.net.URLClassLoader urls) for(var url:urls.getURLs()) paths.add(Path.of(url.toURI()).toString());
    if(paths.isEmpty()) paths.add(System.getProperty("java.class.path"));
    var child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx1g","-cp",
        String.join(java.io.File.pathSeparator,paths),getClass().getName(),dir.toString(),id,out.resolve("recovered.json").toString())
        .redirectErrorStream(true).redirectOutput(out.resolve("recovery.log").toFile()).start();
    try { assertThat(child.waitFor(120,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); assertThat(child.exitValue()).withFailMessage(Files.readString(out.resolve("recovery.log"))).isZero(); }
    finally {if(child.isAlive()) child.destroyForcibly();}
    var recovered=fixture.json.readTree(Files.readString(out.resolve("recovered.json")));
    assertThat(recovered.get("methods")).isEqualTo(fixture.json.valueToTree(before));
    result.put("recoveryMs",recovered.get("recoveryMs")); result.put("recoveredMemory",recovered.get("memory"));
    try(var files=Files.list(dir)) {result.put("diskBytes",files.mapToLong(p->{try{return Files.size(p);}catch(Exception e){throw new IllegalStateException(e);}}).sum());}
    Files.writeString(out.resolve("result.json"),fixture.json.writeValueAsString(result));
  }
}



