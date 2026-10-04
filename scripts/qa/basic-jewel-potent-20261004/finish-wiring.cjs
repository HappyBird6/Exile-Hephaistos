const fs=require('fs');
function edit(p,f){fs.writeFileSync(p,f(fs.readFileSync(p,'utf8')))}
const dir='frontend/src/features/crafting/';
edit(dir+'draft.ts',s=>s.replaceAll("| 'sapphire'","| 'ruby' | 'emerald' | 'diamond' | 'sapphire'").replace('const baseTexts = {',"const baseTexts = {\n  ruby: 'Item Class: Jewels\\nRarity: Normal\\nRuby',\n  emerald: 'Item Class: Jewels\\nRarity: Normal\\nEmerald',\n  diamond: 'Item Class: Jewels\\nRarity: Normal\\nDiamond',"));
edit(dir+'CraftingPage.tsx',s=>s.replace('baseNames[catalogBase]','baseSlugs[catalogBase]').replace("castSpeed === null\n                  ? []", "castSpeed === null || base !== 'sapphire'\n                  ? []").replace("                        sapphireAffix && sapphireRarity !== 'NORMAL'\n                          ? Number(sapphireRoll)","                        baseChoice === 'sapphire' && sapphireAffix && sapphireRarity !== 'NORMAL'\n                          ? Number(sapphireRoll)").replace('                  detail: concrete.catalystQuality','                  detail: concrete.catalystQuality || projection.status === \'JEWEL_EFFECT_AND_QUALITY_PROVISIONAL\''));
edit(dir+'workbenchApi.ts',s=>s.replace("import liquidTargets from", "import displayBindings from '../../shared/i18n/modifierTemplates.json'\nimport liquidTargets from").replace('): string {\n  if (\n    definition.stats',`): string {
  if (/^(ruby|emerald|sapphire|diamond):/.test(definition.id)) {
    const binding = (displayBindings.definitions as Record<string,{englishText:string;values:string[];template:string}>)[definition.id]
    if(binding && binding.englishText===definition.text && definition.stats && !definition.tags?.includes('unscalable')) {
      let text=definition.text
      for(const [i,n] of binding.values.entries()) {
        const stat=definition.stats[i]
        if(stat && Number.isSafeInteger(values[stat.id])) {
          const replacement=n.startsWith('+') ? '+'+values[stat.id] : String(values[stat.id])
          text=text.replace(n,replacement)
        }
      }
      return text
    }
  }
  if (
    definition.stats`));
edit(dir+'localizedModifiers.ts',s=>s.replace('  const numbers = [...binding.values]\n  if (values)',`  const numbers = [...binding.values]
  if (values && /^(ruby|emerald|sapphire|diamond):/.test(definition.id)) {
    if(!definition.tags?.includes('unscalable')) numbers.forEach((n,i)=>{
      const stat=definition.stats?.[i]
      if(stat && Number.isSafeInteger(values[stat.id]))numbers[i]=(n.startsWith('+')?'+':'')+formatNumber(values[stat.id]!,{maximumFractionDigits:20},locale)
    })
    return translation.template.replace(/\\{v(\\d+)\\}/g,(match,index:string)=>numbers[Number(index)]??match)
  }
  if (values)`));
edit('backend/src/test/java/com/poe2craft/crafting/SapphireGenerationLiquidTest.java',s=>s.replace('.hasSize(10);','.hasSize(15);').replace('var target = action.replacementModifiers().getFirst();','var target = result.events().getLast().modifierId();\n      assertThat(action.replacementModifiers()).contains(target);'));
edit(dir+'localizedModifiers.test.ts',s=>s.replace('toHaveLength(68)','toHaveLength(73)'));
edit(dir+'sapphireJewel.ts',s=>s.replace("import definitions from './sapphireDefinitions.json'", "import { reviewedBasicJewel } from './basicJewel'").replace(/export function reviewedSapphire[\s\S]*/, 'export function reviewedSapphire(state: ConcreteItem): boolean {\n  return state.baseItemId === sapphireBase && reviewedBasicJewel(state)\n}\n'));
// Supersede the Sapphire-only scope notice, preserving six-language UI.
const mf='frontend/src/shared/i18n/messages.json',m=JSON.parse(fs.readFileSync(mf));
const texts={en:'Basic Jewels: Ruby 50, Emerald 74, Sapphire 58, Diamond 160 ordinary candidates; Magic 1P/1S, Rare 2P/2S. Only sourced per-base Liquid outcomes are supported. Odds are modeled 1/N after eligibility filtering; game weights are unavailable. One Crafted modifier. Ferocity and quality use a provisional combined display projection. Contempt expands the opposite side; removing it preserves existing excess affixes but restricts future insertion. Melancholy conditions are displayed only. Ancient Liquids remain unsupported.',ko:'Basic Jewel: Ruby 50·Emerald 74·Sapphire 58·Diamond 160 일반 후보, Magic 1P/1S·Rare 2P/2S. Base별 출처가 확인된 Liquid만 지원합니다. 게임 weight가 없어 적격 후보 필터 후 1/N 모델을 사용합니다. Crafted는 하나입니다. Ferocity와 quality는 잠정 결합 표시 모델입니다. Contempt는 반대쪽 슬롯을 늘리며 제거 후 기존 초과 옵션은 보존하고 새 삽입만 제한합니다. Melancholy는 조건만 표시합니다. Ancient Liquid는 미지원입니다.','zh-CN':'Basic Jewel：Ruby 50、Emerald 74、Sapphire 58、Diamond 160 个普通候选；Magic 1P/1S，Rare 2P/2S。仅支持有来源的各底材 Liquid。游戏权重未知，筛选后使用 1/N 模型。最多一个 Crafted。Ferocity 与品质采用暂定组合显示模型。Contempt 扩展另一侧词缀上限，移除后保留现有超限词缀，仅限制新添加。Melancholy 仅显示条件。Ancient Liquid 暂不支持。','zh-TW':'Basic Jewel：Ruby 50、Emerald 74、Sapphire 58、Diamond 160 個普通候選；Magic 1P/1S，Rare 2P/2S。僅支援有來源的各底材 Liquid。遊戲權重未知，篩選後使用 1/N 模型。最多一個 Crafted。Ferocity 與品質採用暫定組合顯示模型。Contempt 擴展另一側詞綴上限，移除後保留現有超限詞綴，僅限制新添加。Melancholy 僅顯示條件。Ancient Liquid 暫不支援。',ja:'Basic Jewel：Ruby 50、Emerald 74、Sapphire 58、Diamond 160 の通常候補。Magic 1P/1S、Rare 2P/2S。出典のあるベース別 Liquid のみ対応。ゲームの重みは不明のため適格候補に 1/N モデルを使用します。Crafted は1個。Ferocity と品質は暫定の合成表示モデルです。Contempt は反対側の上限を増やし、削除後も既存の超過は保持し新規追加のみ制限します。Melancholy は条件のみ表示。Ancient Liquid は未対応。',es:'Basic Jewels: Ruby 50, Emerald 74, Sapphire 58, Diamond 160 candidatos ordinarios; Magic 1P/1S, Rare 2P/2S. Solo resultados Liquid documentados por base. Sin pesos del juego, se usa 1/N tras filtrar. Un Crafted máximo. Ferocity y calidad usan una proyección combinada provisional. Contempt amplía el lado opuesto; al quitarlo conserva los afijos excedentes y limita nuevas inserciones. Melancholy solo muestra condiciones. Ancient Liquid no compatible.'};
for(const [l,t]of Object.entries(texts))m[l]['notice.sapphire_scope']=t;
fs.writeFileSync(mf,JSON.stringify(m,null,2)+'\n');
