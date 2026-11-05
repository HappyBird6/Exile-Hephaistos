from pathlib import Path
import urllib.request, hashlib, json, re, datetime
root=Path(__file__).resolve().parents[5]
dest=root/'data-pipeline/captures/0.5.5/sanctification'
dest.mkdir(parents=True,exist_ok=True)
results=[]
for name in ['Currency','Quality','Omen','Sanctified_Items','Stocky_Mitts','Armourers_Scrap','Blacksmiths_Whetstone','Omen_of_Sanctification']:
    url='https://poe2db.tw/us/'+name
    try:
        file=dest/(name+'.html')
        if file.exists(): raw=file.read_bytes()
        else:
            raw=urllib.request.urlopen(urllib.request.Request(url,headers={'User-Agent':'Mozilla/5.0'}),timeout=30).read()
            file.write_bytes(raw)
        sha=hashlib.sha256(raw).hexdigest()
        results.append(dict(url=url,sha256=sha,bytes=len(raw),capturedAt=datetime.datetime.fromtimestamp(file.stat().st_mtime,datetime.timezone.utc).isoformat(),localCapture=str(file)))
        if name=='Currency':
            text=raw.decode('utf-8')
            for pattern in ['rarity', 'normal items', 'magic items', '5%', '2%', 'CurrencyArmourQuality']:
                matches=list(re.finditer(re.escape(pattern),text,re.I))
                print(name,pattern,len(matches))
                if pattern in ['normal items','magic items']: print([re.sub('<[^>]*>','',text[max(0,m.start()-120):m.end()+200]) for m in matches[:2]])
    except Exception as error: results.append(dict(url=url,error=type(error).__name__))
evidence=root/'docs/evidence/0.5.5/crafting/sanctification/sources/source-captures.json'
evidence.write_text(json.dumps(results,indent=2)+'\n',encoding='utf-8')
print('Public capture digests recorded; raw HTML remains ignored.')
