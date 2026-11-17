import com.poe2craft.item.*;
import com.poe2craft.item.infrastructure.*;
import com.poe2craft.crafting.domain.*;
import com.poe2craft.crafting.domain.goalfilter.GoalFilter.Status;
import com.poe2craft.crafting.domain.pathsearch.*;
import com.poe2craft.crafting.infrastructure.*;
import java.util.*;
public class ScopeProbe {
  static final String A="projectile_skill_gem_level_+", B="base_spirit_from_equipment";
  static long value(ItemState item,String stat){return item.explicits().stream().mapToLong(m->m.values().getOrDefault(stat,0L)).sum();}
  public static void main(String[] args) throws Exception {
    WorkbenchDefinitionsLoader.initialize();
    var catalog=ItemCatalogLoader.loadDefault();
    var initial=SolarAmulet.initial(catalog,82,15);
    var root=new ItemState(initial.snapshotId(),initial.baseItemId(),82,ItemState.Rarity.RARE,initial.implicits(),List.of(),Set.of());
    var coupled=catalog.modifiers().values().stream().filter(m->m.stats().size()>1).map(ModifierDefinition::id).collect(java.util.stream.Collectors.toSet());
    var policy=AutomaticCurrencyPolicyLoader.load();
    var goals=new com.poe2craft.crafting.application.goalfilter.GoalFilterService(new com.poe2craft.crafting.domain.goalfilter.GoalCatalogIndex(List.of(catalog),com.poe2craft.crafting.infrastructure.goalfilter.GoalDefinitionsLoader.load()));
    var goal=new com.poe2craft.crafting.domain.goalfilter.GoalFilter(1,goals.catalog(new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Context(root.snapshotId(),root.baseItemId(),82)).catalogVersion(),new com.poe2craft.crafting.domain.goalfilter.GoalFilter.General(root.baseItemId(),new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(null,null),List.of(ItemState.Rarity.RARE)),List.of(new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Group("g",com.poe2craft.crafting.domain.goalfilter.GoalFilter.Type.AND,false,null,List.of(new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Entry("a","hephaistos:v1:explicit:"+A,"flat",new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(java.math.BigDecimal.valueOf(3),null),null,false),new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Entry("b","hephaistos:v1:explicit:"+B,"flat",new com.poe2craft.crafting.domain.goalfilter.GoalFilter.Range(java.math.BigDecimal.valueOf(47),null),null,false)))));
    var kernel=new AutomaticCraftingKernel(policy,catalog,new WorkbenchSimulator(catalog,new CraftingEngine(catalog),coupled),"amulets").withRollPartition(goals.rollPartition(root,goal),goal.toString());
    var rows=new ArrayList<Map<String,Object>>();
    for(var action:AutomaticCraftingAction.candidates(policy)){
      long start=System.nanoTime();var result=kernel.expand(root,action,2000);
      rows.add(Map.of("currency",action.currency().name(),"omens",action.omens(),"purpose",action.purpose(),"status",result.status(),"reason",result.reason(),"outcomes",result.outcomes().size(),"unresolved",result.unresolved(),"ms",(System.nanoTime()-start)/1e6));
    }
    java.util.function.Function<ItemState,AutomaticMethodGraph.Progress> evaluate=item->{boolean a=value(item,A)>=3,b=value(item,B)>=47;return new AutomaticMethodGraph.Progress(a&&b?Status.MATCH:Status.NO_MATCH,List.of(a?Status.MATCH:Status.NO_MATCH,b?Status.MATCH:Status.NO_MATCH));};
    var stats=new ArrayList<Map<String,Object>>();
    var continuations=new ArrayList<ItemState>();
    try(var graph=new AutomaticMethodGraph(root,policy,kernel,evaluate,CertifiedInterval.Precision.standard(),64,128,20000,5000)){
      long start=System.nanoTime();
      for(int round=0;round<2;round++){
        if(round>0)graph.increaseLimits(128,256,50000,10000);
        for(int i=0;i<20000&&!graph.finished()&&!graph.resourceLimited();i++)graph.step();
        graph.publishProgress();
        stats.add(Map.of("round",round,"elapsedMs",(System.nanoTime()-start)/1e6,"nodes",graph.nodes().size(),"methods",graph.methods().size(),"retained",graph.retainedStates(),"complete",graph.complete(),"paused",graph.resourceLimited(),"A",graph.nodes().stream().filter(n->n.progress().entries().equals(List.of(Status.MATCH,Status.NO_MATCH))).count(),"B",graph.nodes().stream().filter(n->n.progress().entries().equals(List.of(Status.NO_MATCH,Status.MATCH))).count(),"C",graph.nodes().stream().filter(n->n.progress().goal()==Status.MATCH).count()));
      }
      for(var match:List.of(List.of(Status.MATCH,Status.NO_MATCH),List.of(Status.NO_MATCH,Status.MATCH)))graph.nodes().stream().filter(n->n.progress().entries().equals(match)).findFirst().ifPresent(n->continuations.add(n.item()));
    }
    var continued=new ArrayList<Map<String,Object>>();
    for(var start:continuations){
      var next=kernel.withRollPartition(goals.rollPartition(start,goal),goal.toString()+start.hashCode());
      try(var graph=new AutomaticMethodGraph(start,policy,next,evaluate,CertifiedInterval.Precision.standard(),64,128,20000,5000)){
        for(int i=0;i<10000&&!graph.finished()&&!graph.resourceLimited();i++)graph.step();graph.publishProgress();
        continued.add(Map.of("start",evaluate.apply(start).entries(),"nodes",graph.nodes().size(),"C",graph.nodes().stream().filter(n->n.progress().goal()==Status.MATCH).count(),"goalExitWitnesses",graph.methods().stream().flatMap(m->m.points().get(100).exits().values().stream()).flatMap(l->l.keySet().stream()).map(AutomaticMethodExecution.State::item).filter(i->evaluate.apply(i).goal()==Status.MATCH).distinct().count(),"complete",graph.complete(),"reasons",graph.methods().stream().flatMap(m->m.unresolvedReasons().stream()).distinct().toList()));
      }
    }
    var output=Map.of("kernel",kernel.identity().kernelVersion(),"allCandidateExecutions",rows.size(),"scope",rows,"boundedGraph",stats,"selectedNodeContinuations",continued,"peakHeapBytes",java.lang.management.ManagementFactory.getMemoryPoolMXBeans().stream().filter(b->b.getType()==java.lang.management.MemoryType.HEAP).mapToLong(b->b.getPeakUsage().getUsed()).sum());
    System.out.println(new com.fasterxml.jackson.databind.ObjectMapper().writerWithDefaultPrettyPrinter().writeValueAsString(output));
  }
}
