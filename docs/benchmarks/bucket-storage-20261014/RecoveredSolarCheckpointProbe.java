package com.poe2craft.crafting.application.pathsearch;

import static org.assertj.core.api.Assertions.*;
import com.poe2craft.crafting.infrastructure.*;
import java.nio.file.*;
import java.time.Clock;
import java.util.*;
import org.junit.jupiter.api.Test;

public class RecoveredSolarCheckpointProbe {
  @Test void restoreInterruptedLargeJobIntoExternalWorkingStores() throws Exception {
    var out=Path.of("/task-cache/bucket-method-verification/solar-interrupted-recovery");
    var source=Path.of("/task-cache/bucket-method-verification/durable-solar-continuation/jobs");
    var dir=out.resolve("jobs"); Files.createDirectories(dir);
    try(var files=Files.list(source)) {for(var file:files.toList()) Files.copy(file,dir.resolve(file.getFileName()));}
    var fixture=new PathSearchServiceTest(); var repository=new LocalCertifiedJobRepository(dir,fixture.json);
    var descriptor=repository.list().getFirst(); String id=descriptor.id(),execution; long expectedOperations;
    try(var store=repository.open(descriptor)) {
      var cursor=store.execution("job").orElseThrow(); execution=cursor.id();
      var tail=fixture.json.readTree(store.executionRows(execution,cursor.rows()-1,1).rows().getFirst().content());
      assertThat(tail.path("kind").asText()).isEqualTo("QUEUES");
      expectedOperations=tail.path("value").path("operations").asLong();
    }
    long start=System.nanoTime();
    List<CertifiedMethodProtocol.Method> before;
    var result=new LinkedHashMap<String,Object>();
    try(var service=new MethodSearchService(fixture.catalog,fixture.goals,fixture.identity,fixture.json,Clock.systemUTC(),fixture.executor,1000000)) {
      service.persistence(repository); result.put("firstRecoveryMs",(System.nanoTime()-start)/1000000);
      var snapshot=service.getCertified("probe",id);
      assertThat(snapshot.reason()).isEqualTo("PROCESS_RESTART");
      assertThat(snapshot.operations()).isEqualTo(expectedOperations).isGreaterThanOrEqualTo(3000000);
      before=DurableSolarProbe.methods(service,snapshot); result.put("snapshot",snapshot); result.put("memory",DurableSolarProbe.memory());
    }
    var paths=new LinkedHashSet<String>();
    for(ClassLoader loader=getClass().getClassLoader();loader!=null;loader=loader.getParent())
      if(loader instanceof java.net.URLClassLoader urls) for(var url:urls.getURLs()) paths.add(Path.of(url.toURI()).toString());
    if(paths.isEmpty()) paths.add(System.getProperty("java.class.path"));
    var child=new ProcessBuilder(Path.of(System.getProperty("java.home"),"bin","java").toString(),"-Xmx1g","-cp",String.join(java.io.File.pathSeparator,paths),
        DurableSolarProbe.class.getName(),dir.toString(),id,out.resolve("second.json").toString())
        .redirectErrorStream(true).redirectOutput(out.resolve("recovery.log").toFile()).start();
    try {assertThat(child.waitFor(180,java.util.concurrent.TimeUnit.SECONDS)).isTrue(); assertThat(child.exitValue()).withFailMessage(Files.readString(out.resolve("recovery.log"))).isZero();}
    finally {if(child.isAlive()) child.destroyForcibly();}
    var second=fixture.json.readTree(Files.readString(out.resolve("second.json")));
    assertThat(second.get("methods")).isEqualTo(fixture.json.valueToTree(before));
    assertThat(second.path("snapshot").path("operations").asLong()).isEqualTo(expectedOperations);
    try(var store=repository.open(descriptor)) {assertThat(store.execution("job").orElseThrow().id()).isEqualTo(execution);}
    result.put("secondRecoveryMs",second.get("recoveryMs")); result.put("secondMemory",second.get("memory"));
    result.put("executionReferenceUnchanged",true); result.put("methodObservationsEqual",true);
    Files.writeString(out.resolve("result.json"),fixture.json.writeValueAsString(result));
  }
}
