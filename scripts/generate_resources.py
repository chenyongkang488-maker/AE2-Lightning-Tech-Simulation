"""Generate editable JSON defaults, translations and guide pages; artwork is made in Blockbench."""
import json, pathlib, gzip, struct
ROOT=pathlib.Path(__file__).resolve().parents[1]/'src/main/resources'
def write(path,value):
    path=ROOT/path; path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
zh={'item.overload_sim.blank_simulation_crystal':'空白模拟电鸣水晶','item.overload_sim.simulation_crystal':'模拟电鸣水晶','item.overload_sim.perfect_simulation_crystal':'完美模拟电鸣水晶','block.overload_sim.overload_simulation_chamber':'过载模拟室','itemGroup.overload_sim':'AE2 闪电科技：模拟','tooltip.overload_sim.profile':'模拟对象：%s','tooltip.overload_sim.progress':'已培养雷击：%s 次','tooltip.overload_sim.missing':'模拟档案已被移除，暂时无法工作','tooltip.overload_sim.guide':'长按 G 查看 AE2 指南','gui.overload_sim.frequency':'过载频率','gui.overload_sim.eject':'自动弹出','gui.overload_sim.enabled':'开启','gui.overload_sim.disabled':'关闭','gui.overload_sim.power':'FE：%s  并行：%s / %s','gui.overload_sim.slot.0':'完美模拟水晶（可重复使用）','gui.overload_sim.slot.1':'闪电坍缩矩阵：每个提供 4 并行，最多 32 个','gui.overload_sim.slot.2':'AE2 加速卡：最多 4 张','gui.overload_sim.slot.3':'配方辅助材料（默认配方无需材料）'}
en={'item.overload_sim.blank_simulation_crystal':'Blank Simulation Crystal','item.overload_sim.simulation_crystal':'Simulation Crystal','item.overload_sim.perfect_simulation_crystal':'Perfect Simulation Crystal','block.overload_sim.overload_simulation_chamber':'Overload Simulation Chamber','itemGroup.overload_sim':'AE2 Lightning Tech: Simulation','tooltip.overload_sim.profile':'Simulation: %s','tooltip.overload_sim.progress':'Cultivation strikes: %s','tooltip.overload_sim.missing':'Missing simulation profile; operation paused','tooltip.overload_sim.guide':'Hold G to open the AE2 guide','gui.overload_sim.frequency':'Frequency','gui.overload_sim.eject':'Auto eject','gui.overload_sim.enabled':'Enabled','gui.overload_sim.disabled':'Disabled','gui.overload_sim.power':'FE: %s  Parallel: %s / %s','gui.overload_sim.slot.0':'Reusable perfect simulation crystal','gui.overload_sim.slot.1':'Lightning Collapse Matrix: 4 parallel each, up to 32','gui.overload_sim.slot.2':'AE2 acceleration cards: up to 4','gui.overload_sim.slot.3':'Auxiliary recipe input (optional)'}
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
guide='''---
navigation:
  title: AE2 闪电科技：模拟
  icon: overload_sim:overload_simulation_chamber
  parent: ae2:items-blocks-machines/items-blocks-machines-index.md
item_ids:
  - overload_sim:blank_simulation_crystal
  - overload_sim:simulation_crystal
  - overload_sim:perfect_simulation_crystal
  - overload_sim:overload_simulation_chamber
---

# AE2 闪电科技：模拟

<ItemImage id="overload_sim:blank_simulation_crystal" scale="3" />

空白模拟电鸣水晶在闪电科技原生闪电模拟室中合成：1个电鸣水晶、16个过载水晶、16个紫水晶碎片，消耗200,000 FE及32个高压闪电。每次产出1个。

## 雷击绑定

把空白水晶放入闪电收集器。以收集器为中心，在同一高度的 5×5 平面填满 24 个相同的粗矿块；钻石和下界合金使用对应储存块。收集器成功收集雷击后，这些方块被消耗，水晶记录对应矿物。

作物使用 24 格耕地上方已种下的同类植株，生长阶段可以不同。支持神秘农业及其它标准耕地作物、模组耕地和明确支持种植的种植床；绑定只消耗植株，双格植株连同上半部分消耗，不掉落采收物，保留土地。树木使用 24 格泥土类方块上的同类树苗。公共原矿/矿石标签可以自动适配其它模组；含多个候选材料或异质掉落表时停止绑定，作者可写显式规则。

通用作物水晶按成熟植株的正常采收生成产物，包括神秘农业精华、种子及正常副产物，可用于两种模拟室。来源土地等级、crux和环境加成不会被复制；特殊植株或方块实体可由整合包作者使用标签、配方与成熟状态 API 适配，已有显式配方优先。

左手拿空白水晶受到雷击时，有 33% 概率记录半径 5 格内最近的存活生物。只判定真实雷击命中，无需受到伤害，创造模式也可使用；人工雷同样有效。只记录生物类型，不复制装备、背包或个体数据。默认禁止凋灵和末影龙。

已绑定水晶再接受 10 次有效雷击后成为完美水晶。可以继续放在收集器中培养，也可以拿在左手受雷击。自然雷、指令雷和人工雷都可绑定与培养；整合包作者可通过 allow_artificial=false 禁止人工雷。

## 模拟室

模拟室接入在线 ME 网络并取得频道，使用高压闪电（HV），并按 AE2 标准换算持续从 ME 网络充入 FE；待机与加工时都会充电，默认每 tick 最多 10000 FE，满 2000000 FE 时停止。也支持外部 FE 供电。将完美水晶放入第一个槽，模板可重复使用。默认一次操作需要 1000 FE、1 HV 和 200 tick；下界合金成本更高。

闪电坍缩矩阵放第二槽：无矩阵为 1 并行，每个矩阵提供 4 并行，最多 32 个即 128 并行。右上角展开的升级栏有 4 个独立槽，每格放一张 AE2 加速卡，每张将耗时减半。也可以直接手持加速卡右键机器插入；加工中的批次保留原速度，新卡下一批生效。辅助材料槽供自定义配方消耗材料。旧版堆叠卡槽会自动迁移为独立卡槽。

能量和辅助材料在批次开始时按实际并行一次扣除。随机战利品每次操作独立抽取，开始后固定并随任务保存。输出堵塞时等待空间；网络离线时暂停。拆除尚未完成的任务会丢失该任务，不能提前取得产物。

自动弹出可开关，并单独开关六个方向。AE2 输入/输出总线可通过标准物品接口运输。频率按钮使用闪电科技共享的无线过载控制器界面。

生物产物按“无玩家参与的死亡”战利品表抽取，因此依赖玩家击杀、抢夺或特殊个体装备的掉落不会自动出现。

## 整合包配置

数据包可覆盖 simulation_profile、crystal_binding、mob_crystal_binding、crystal_cultivation 和 overload_simulation 配方。支持优先级、标签、概率、能耗、耗时和命名 Java 扩展提供器。修改后执行 /reload。相同最高优先级的匹配配方存在冲突时会停止处理。
'''
for language in ['', '_zh_cn/']:
    path=ROOT/('assets/overload_sim/ae2guide/'+language+'overload-simulation.md');path.parent.mkdir(parents=True,exist_ok=True);path.write_text(guide,encoding='utf-8')
# Minimal gzipped vanilla structure template (12×8×12, no blocks) for real server GameTests.
def utf(s):b=s.encode();return struct.pack('>H',len(b))+b
def tag(t,n,p):return bytes([t])+utf(n)+p
payload=tag(3,'DataVersion',struct.pack('>i',3955))+tag(9,'size',bytes([3])+struct.pack('>i',3)+struct.pack('>iii',12,8,12))+tag(9,'palette',bytes([10])+struct.pack('>i',1)+tag(8,'Name',utf('minecraft:air'))+b'\0')+tag(9,'blocks',bytes([10])+struct.pack('>i',0))+tag(9,'entities',bytes([10])+struct.pack('>i',0))+b'\0'
path=ROOT/'data/overload_sim/structure/empty.nbt';path.parent.mkdir(parents=True,exist_ok=True);path.write_bytes(gzip.compress(b'\x0a\x00\x00'+payload,mtime=0))
print('Generated 17 profiles, 4 recipe types, crafting, models, languages and guides.')
# Restore the tool translations/recipes after regenerating the base resources.
import runpy
runpy.run_path(str(pathlib.Path(__file__).with_name('generate-coil-resources.py')))
