"""Assemble the exact proposed common connection in an ignored, isolated verification copy."""
from pathlib import Path
import difflib, json, shutil, hashlib

root = Path(__file__).resolve().parents[5]
module = root / 'frontend/src/features/crafting/sanctification'
history = root / 'frontend/src/features/crafting/workbenchHistory.ts'
before = history.read_text(encoding='utf-8')
after = before.replace('!supportsConcreteStateShape(state) ||', '!supportsConcreteStateShape(state) ||\n      !validSanctification(state, initial.modifiers) ||').replace(
    "state.conditions[0] === 'CORRUPTED' &&\n          state.desecrated?.length",
    "(state.conditions[0] === 'SANCTIFIED' ||\n            (state.conditions[0] === 'CORRUPTED' && state.desecrated?.length))")
after = "import { validSanctification } from './sanctification/sanctification'\n" + after
(module/'history.patch').write_text(''.join(difflib.unified_diff(before.splitlines(True), after.splitlines(True), fromfile='a/frontend/src/features/crafting/workbenchHistory.ts', tofile='b/frontend/src/features/crafting/workbenchHistory.ts')), encoding='utf-8')
overlays={}
def patch(name, after, target):
    before=(root/name).read_text(encoding='utf-8')
    overlays[name]=after
    (module/target).write_text(''.join(difflib.unified_diff(before.splitlines(True),after.splitlines(True),fromfile='a/'+name,tofile='b/'+name)),encoding='utf-8')
name='backend/src/main/java/com/poe2craft/bootstrap/CraftingConfiguration.java'
before=(root/name).read_text(encoding='utf-8')
after=before.replace('  abstract static class ItemStateJson {', '''  abstract static class ItemStateJson {
    @com.fasterxml.jackson.annotation.JsonInclude(
        com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL)
    abstract com.poe2craft.item.ItemState.Sanctification sanctification();
''')
patch(name,after,'serialization.patch')
name='backend/src/main/java/com/poe2craft/crafting/presentation/WorkbenchController.java'
before=(root/name).read_text(encoding='utf-8')
after=before.replace('            "observations",', '            "observations",\n            "sanctification",')
after=after.replace('    var equipmentQuality = input.get("equipmentQuality");', '''    var sanctification = input.get("sanctification");
    if (sanctification != null && !sanctification.isNull()) {
      if (!sanctification.isObject() || sanctification.size() != 3
          || !sanctification.path("policy").isTextual()) throw new UnsupportedItemProperties();
      for (String layer : List.of("implicits", "explicits")) {
        var factors = sanctification.path(layer);
        if (!factors.isObject()) throw new UnsupportedItemProperties();
        for (var modifier : factors) {
          if (!modifier.isObject()) throw new UnsupportedItemProperties();
          for (var factor : modifier) {
            if (!factor.isIntegralNumber() || !factor.canConvertToInt()
                || factor.intValue() < 78 || factor.intValue() > 122) throw new UnsupportedItemProperties();
          }
        }
      }
    }
    var equipmentQuality = input.get("equipmentQuality");''')
after=after.replace('concreteState(request.state()), request.action(), request.activeOmens(), random);', '''concreteState(request.state(), request.action() == WorkbenchCurrency.DIVINE
            && request.activeOmens() != null && request.activeOmens().contains("Omen_of_Sanctification")),
        request.action(), request.activeOmens(), random);''')
after=after.replace('simulator.actions(concreteState(request.state()), request.activeOmens())', '''simulator.actions(concreteState(request.state(), true), request.activeOmens())''')
after=after.replace('simulator.qualityDisplay(concreteState(request.state()))', 'simulator.qualityDisplay(concreteState(request.state(), true))')
patch(name,after,'controller.patch')
dest = root / '.toolchains/quality-c-candidate'
dest.mkdir(parents=True, exist_ok=True)
shutil.copytree(root/'backend', dest/'backend', dirs_exist_ok=True, ignore=shutil.ignore_patterns('build', '.gradle'))
shutil.copytree(root/'contracts', dest/'contracts', dirs_exist_ok=True)
for name,contents in overlays.items(): (dest/name).write_text(contents,encoding='utf-8')
definitions = dest/'backend/src/main/java/com/poe2craft/crafting/domain/WorkbenchDefinitions.java'
definitions.write_text(definitions.read_text(encoding='utf-8').replace('case BLESSED ->','case BLESSED, SANCTIFICATION ->'), encoding='utf-8')
resource = dest/'backend/src/main/resources/crafting/workbench-definitions-v1.json'
data=json.loads(resource.read_text(encoding='utf-8'))
for id in ['ARMOURERS_SCRAP','BLACKSMITHS_WHETSTONE']:
    data['currencies'].append(dict(id=id,baseAction=None,minimumModifierLevel=0,fixedModifierId=None,essenceModifierIds=[],essenceChoiceSource=None,replacementModifiers=[],replacementEssenceSource=None,replacementSource=None))
data['omens'].append(dict(key='SANCTIFICATION',id='Omen_of_Sanctification',trigger='DIVINE',affix=None,supportsTieredCurrency=False))
resource.write_text(json.dumps(data,indent=2,ensure_ascii=False)+'\n',encoding='utf-8',newline='\n')
manifest=dest/'backend/src/main/resources/crafting/ruleset-v1.json'
seal=json.loads(manifest.read_text(encoding='utf-8'))
seal['files']['crafting/workbench-definitions-v1.json']=hashlib.sha256(resource.read_bytes()).hexdigest()
manifest.write_text(json.dumps(seal,indent=2,ensure_ascii=False)+'\n',encoding='utf-8',newline='\n')
print('Candidate assembled with the proposed common/controller/serialization patches and one exact resource seal digest. No test expectations or exclusions changed.')
