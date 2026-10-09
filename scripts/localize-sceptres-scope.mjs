import fs from 'node:fs'
import assert from 'node:assert/strict'
const p='frontend/src/shared/i18n/messages.json',messages=JSON.parse(fs.readFileSync(p,'utf8'))
const text={
 en:'Spirit and the granted skill are source base facts. Combat effects, applied quality, sockets and pasted mapping are not simulated. Item-level gates use source modifier levels; lower-level game behavior is unverified.',
 ko:'정신력과 부여된 스킬은 출처의 베이스 정보입니다. 전투 효과, 퀄리티 적용, 소켓, 붙여넣기 매핑은 시뮬레이션하지 않습니다. 레벨 제한은 출처 속성 레벨을 사용하며 낮은 레벨 게임 동작은 미검증입니다.',
 ja:'スピリットと付与スキルは出典のベース情報です。戦闘効果、品質適用、ソケット、貼り付け対応はシミュレーションしません。レベル制限は出典モッドレベルを使用し、低レベル動作は未検証です。',
 'zh-CN':'精神和授予技能是来源基底信息。不模拟战斗效果、施加品质、插槽及粘贴映射。等级限制使用来源词缀等级；低等级游戏行为未经验证。',
 'zh-TW':'精魂與賦予技能是來源基底資料。不模擬戰鬥效果、施加品質、插槽及貼上對應。等級限制使用來源詞綴等級；低等級遊戲行為未經驗證。',
 es:'El espíritu y la habilidad otorgada son datos de la base según la fuente. No se simulan efectos de combate, calidad aplicada, engarces ni correspondencia de texto pegado. Los límites usan niveles de modificadores de la fuente; el comportamiento a niveles inferiores no está verificado.',
}
for(const [locale,value] of Object.entries(text)) {
 assert(!Object.hasOwn(messages[locale],'notice.sceptre_skill_scope'),'Preserve prior messages')
 messages[locale]['notice.sceptre_skill_scope']=value
}
fs.writeFileSync(p,JSON.stringify(messages,null,2)+'\n')
