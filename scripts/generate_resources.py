"""Generate editable JSON defaults and translations; GuideME pages are hand maintained."""
import json, pathlib, gzip, struct
ROOT=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
    path=ROOT/path; path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
zh={'item.overload_sim.blank_simulation_crystal':'空白模拟电鸣水晶','item.overload_sim.simulation_crystal':'模拟电鸣水晶','item.overload_sim.perfect_simulation_crystal':'完美模拟电鸣水晶','block.overload_sim.overload_simulation_chamber':'过载模拟室','itemGroup.overload_sim':'闪电科技：模拟','tooltip.overload_sim.profile':'模拟对象：%s','tooltip.overload_sim.progress':'已培养雷击：%s 次','tooltip.overload_sim.missing':'模拟档案已被移除，暂时无法工作','tooltip.overload_sim.guide':'长按 G 查看 AE2 指南','gui.overload_sim.frequency':'过载频率','gui.overload_sim.eject':'自动弹出','gui.overload_sim.enabled':'开启','gui.overload_sim.disabled':'关闭','gui.overload_sim.power':'FE：%s  并行：%s / %s','gui.overload_sim.slot.0':'完美模拟水晶（可重复使用）','gui.overload_sim.slot.1':'闪电坍缩矩阵：每个提供 4 并行，最多 32 个','gui.overload_sim.slot.2':'AE2 加速卡：最多 4 张','gui.overload_sim.slot.3':'配方辅助材料（默认配方无需材料）'}
en={'item.overload_sim.blank_simulation_crystal':'Blank Simulation Crystal','item.overload_sim.simulation_crystal':'Simulation Crystal','item.overload_sim.perfect_simulation_crystal':'Perfect Simulation Crystal','block.overload_sim.overload_simulation_chamber':'Overload Simulation Chamber','itemGroup.overload_sim':'Lightning Tech: Simulation','tooltip.overload_sim.profile':'Simulation: %s','tooltip.overload_sim.progress':'Cultivation strikes: %s','tooltip.overload_sim.missing':'Missing simulation profile; operation paused','tooltip.overload_sim.guide':'Hold G to open the AE2 guide','gui.overload_sim.frequency':'Frequency','gui.overload_sim.eject':'Auto eject','gui.overload_sim.enabled':'Enabled','gui.overload_sim.disabled':'Disabled','gui.overload_sim.power':'FE: %s  Parallel: %s / %s','gui.overload_sim.slot.0':'Reusable perfect simulation crystal','gui.overload_sim.slot.1':'Lightning Collapse Matrix: 4 parallel each, up to 32','gui.overload_sim.slot.2':'AE2 acceleration cards: up to 4','gui.overload_sim.slot.3':'Auxiliary recipe input (optional)'}
for i,(a,b) in enumerate(zip(['待机','模拟中','等待 ME 网络上线','能量或材料不足','等待输出空间','配方缺失或冲突','档案缺失，已暂停','等待返还闪电能量'],['Idle','Simulating','Waiting for active ME network','Insufficient energy or input','Waiting for output space','Missing or ambiguous recipe','Missing profile; paused','Returning lightning energy'])):zh[f'status.overload_sim.{i}']=a;en[f'status.overload_sim.{i}']=b
for i,(a,b) in enumerate(zip(['下','上','北','南','西','东'],['D','U','N','S','W','E'])):zh[f'direction.overload_sim.{i}']=a;en[f'direction.overload_sim.{i}']=b
zh.update({'gui.overload_sim.output_config':'输出面配置','gui.overload_sim.back':'返回模拟室','gui.overload_sim.clear_sides':'关闭所有输出面','gui.overload_sim.network_power':'在线时持续从 AE 网络充入 FE','gui.overload_sim.hv':'网络高压闪电：%s','gui.overload_sim.energy':'FE：%s','gui.overload_sim.parallel':'并行：%s / %s'})
en.update({'gui.overload_sim.output_config':'Outputs','gui.overload_sim.back':'Return to Chamber','gui.overload_sim.clear_sides':'Disable all output faces','gui.overload_sim.network_power':'Continuously charges FE from an online ME grid','gui.overload_sim.hv':'Network HV: %s','gui.overload_sim.energy':'FE: %s','gui.overload_sim.parallel':'Parallel: %s / %s','gui.overload_sim.eject':'Eject','gui.overload_sim.enabled':'On','gui.overload_sim.disabled':'Off'})
defaults=[('iron','mineral','raw_iron_block','raw_iron',1,'粗铁','Raw Iron'),('copper','mineral','raw_copper_block','raw_copper',1,'粗铜','Raw Copper'),('gold','mineral','raw_gold_block','raw_gold',1,'粗金','Raw Gold'),('diamond','mineral','diamond_block','diamond',1,'钻石','Diamond'),('netherite','mineral','netherite_block','netherite_ingot',1,'下界合金','Netherite'),('wheat','crop','wheat','wheat',2,'小麦','Wheat'),('carrot','crop','carrots','carrot',2,'胡萝卜','Carrot'),('potato','crop','potatoes','potato',2,'马铃薯','Potato'),('beetroot','crop','beetroots','beetroot',2,'甜菜根','Beetroot')]
for tree,cn in [('oak','橡树'),('spruce','云杉'),('birch','白桦'),('jungle','丛林树'),('acacia','金合欢'),('dark_oak','深色橡树'),('cherry','樱花树')]:defaults.append((tree,'tree',tree+'_sapling',tree+'_log',4,cn,tree.replace('_',' ').title()))
for key,kind,block,item,count,cn,name in defaults:
    profile='overload_sim:'+key
    write(pathlib.Path('data/overload_sim/simulation_profile')/(key+'.json'),{'kind':kind,'name':'profile.overload_sim.'+key,'icon':'minecraft:'+item})
    rule={'mode':kind,'material':{'id':'minecraft:'+block}}
    if kind=='crop':rule['soil']={'id':'minecraft:farmland'}
    if kind=='tree':rule['soil']={'id':'#minecraft:dirt'}
    write(pathlib.Path('data/overload_sim/recipe/binding')/(key+'.json'),{'type':'overload_sim:crystal_binding','profile':profile,'world':rule})
    cost=1000 if key!='netherite' else 16000
    outputs=[{'item':'minecraft:'+item,'count':count}]
    if key in ['wheat','beetroot']:outputs.append({'item':'minecraft:'+key+'_seeds','count':1,'chance':.5})
    write(pathlib.Path('data/overload_sim/recipe/production')/(key+'.json'),{'type':'overload_sim:overload_simulation','profile':profile,'production':{'ticks':200,'fe':cost,'lightning':1,'outputs':outputs}})
    zh['profile.overload_sim.'+key]=cn;en['profile.overload_sim.'+key]=name
write(pathlib.Path('data/overload_sim/simulation_profile/mob.json'),{'kind':'mob','name':'profile.overload_sim.mob','icon':'minecraft:rotten_flesh'})
zh['profile.overload_sim.mob']='生物战利品';en['profile.overload_sim.mob']='Mob loot'
write(pathlib.Path('data/overload_sim/recipe/binding/mob.json'),{'type':'overload_sim:mob_crystal_binding','profile':'overload_sim:mob','mob':{'entity':'*','radius':5,'probability':.33}})
for mob in ['wither','ender_dragon']:
    write(pathlib.Path('data/overload_sim/recipe/binding')/('deny_'+mob+'.json'),{'type':'overload_sim:mob_crystal_binding','profile':'overload_sim:mob','priority':1000,'mob':{'entity':'minecraft:'+mob,'disabled':True}})
write(pathlib.Path('data/overload_sim/recipe/cultivation/default.json'),{'type':'overload_sim:crystal_cultivation','profile':'overload_sim:any','cultivation':{'required':10,'increment':1}})
write(pathlib.Path('data/overload_sim/recipe/production/mob.json'),{'type':'overload_sim:overload_simulation','profile':'overload_sim:mob','production':{'ticks':200,'fe':1000,'lightning':1,'entity_loot':True}})
from manufacturing_recipes import write_recipes
write_recipes()
for name in ['blank_simulation_crystal','simulation_crystal','perfect_simulation_crystal']:
    textures={'layer0':'overload_sim:item/'+name}
    if name=='perfect_simulation_crystal':textures['layer1']='overload_sim:item/perfect_simulation_crystal_lightning'
    write(pathlib.Path('assets/overload_sim/models/item')/(name+'.json'),{'parent':'minecraft:item/generated','textures':textures})
write(pathlib.Path('assets/overload_sim/blockstates/overload_simulation_chamber.json'),{'variants':{'':{'model':'overload_sim:block/overload_simulation_chamber'}}})
# The chamber geometry is exported and edited in Blockbench. Keep that source intact.
chamber_model=pathlib.Path('assets/overload_sim/models/block/overload_simulation_chamber.json')
if not (ROOT/chamber_model).exists():write(chamber_model,{'parent':'minecraft:block/cube_all','textures':{'all':'overload_sim:block/overload_simulation_chamber'}})
write(pathlib.Path('assets/overload_sim/models/item/overload_simulation_chamber.json'),{'parent':'overload_sim:block/overload_simulation_chamber'})
for tag in ['mineable/pickaxe','needs_iron_tool']:write(pathlib.Path('data/minecraft/tags/block')/(tag+'.json'),{'replace':False,'values':['overload_sim:overload_simulation_chamber']})
write(pathlib.Path('data/overload_sim/loot_table/blocks/overload_simulation_chamber.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'overload_sim:overload_simulation_chamber'}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(pathlib.Path('assets/overload_sim/lang/zh_cn.json'),zh);write(pathlib.Path('assets/overload_sim/lang/en_us.json'),en)
# GuideME pages are hand maintained; resource generation must not overwrite them.
# Minimal gzipped vanilla structure template (12×8×12, no blocks) for real server GameTests.
def utf(s):b=s.encode();return struct.pack('>H',len(b))+b
def tag(t,n,p):return bytes([t])+utf(n)+p
payload=tag(3,'DataVersion',struct.pack('>i',3955))+tag(9,'size',bytes([3])+struct.pack('>i',3)+struct.pack('>iii',12,8,12))+tag(9,'palette',bytes([10])+struct.pack('>i',1)+tag(8,'Name',utf('minecraft:air'))+b'\0')+tag(9,'blocks',bytes([10])+struct.pack('>i',0))+tag(9,'entities',bytes([10])+struct.pack('>i',0))+b'\0'
path=ROOT/'data/overload_sim/structure/empty.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\x00\x00'+payload,mtime=0))
print('Generated 17 profiles, 4 recipe types, crafting, models and languages; GuideME pages left unchanged.')
# Restore the tool translations/recipes after regenerating the base resources.
import runpy
runpy.run_path(str(pathlib.Path(__file__).with_name('generate-coil-resources.py')))
