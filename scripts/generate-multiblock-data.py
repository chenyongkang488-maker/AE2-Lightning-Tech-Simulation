"""Write JSON data and guide text only. Pixel assets are authored/exported in Blockbench."""
import json
from pathlib import Path
ROOT=Path(__file__).resolve().parents[1]
RES=ROOT/'src/main/resources'
NAMES={
 'simulation_controller':('模拟室控制器','Simulation Chamber Controller'),
 'simulation_frame':('模拟室框架','Simulation Chamber Frame'),
 'simulation_efficiency_t1':('模拟效率模块 T1','Simulation Efficiency Module T1'),
 'simulation_efficiency_t2':('模拟效率模块 T2','Simulation Efficiency Module T2'),
 'simulation_efficiency_t3':('模拟效率模块 T3','Simulation Efficiency Module T3'),
 'simulation_fortune_module':('模拟时运模块','Simulation Fortune Module'),
 'simulation_overload_module':('模拟过载模块','Simulation Overload Module'),
 'simulation_smelting_module':('模拟熔炼模块','Simulation Smelting Module')}
def write(path,value):
 path=RES/path;path.parent.mkdir(parents=True,exist_ok=True);path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
for block in NAMES:
 write(Path('data/overload_sim/loot_table/blocks')/(block+'.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'overload_sim:'+block}],'conditions':[{'condition':'minecraft:survives_explosion'}]}]})
write(Path('data/overload_sim/loot_table/blocks/formed_simulation_glass.json'),{'type':'minecraft:block','pools':[{'rolls':1,'entries':[{'type':'minecraft:item','name':'ae2:quartz_vibrant_glass'}]}]})
write(Path('data/minecraft/tags/block/mineable/pickaxe.json'),{'replace':False,'values':['overload_sim:'+id for id in NAMES]})
write(Path('data/overload_sim/tags/block/simulation_frames.json'),{'replace':False,'values':['overload_sim:simulation_frame']})
write(Path('data/overload_sim/tags/block/simulation_glass.json'),{'replace':False,'values':['ae2:quartz_vibrant_glass']})
write(Path('data/overload_sim/multiblock_simulation/default.json'),{'policy':{'ticks':180,'reduction_cap':104,'fortune_cap':10,'t1':2,'t2':4,'t3':8,'fe':1000,'hv':1,'overload_ehv':1,'smelting_hv':2},'smelting':[{'input':'minecraft:raw_'+s,'result':'minecraft:'+s+'_ingot','count':2} for s in ('iron','gold','copper')],'modules':[]})
recipes={
 'simulation_frame':(['IQI','QGQ','IQI'],{'I':'minecraft:iron_ingot','Q':'minecraft:quartz','G':'ae2:quartz_vibrant_glass'},4),
 'simulation_controller':(['FFF','FCF','FFF'],{'F':'overload_sim:simulation_frame','C':'overload_sim:overload_simulation_chamber'},1),
 'simulation_efficiency_t1':([' R ','RSR',' F '],{'R':'minecraft:redstone','S':'ae2:speed_card','F':'overload_sim:simulation_frame'},1),
 'simulation_efficiency_t2':([' R ','RMR',' F '],{'R':'minecraft:redstone_block','M':'overload_sim:simulation_efficiency_t1','F':'overload_sim:simulation_frame'},1),
 'simulation_efficiency_t3':([' D ','DMD',' F '],{'D':'minecraft:diamond','M':'overload_sim:simulation_efficiency_t2','F':'overload_sim:simulation_frame'},1),
 'simulation_fortune_module':([' L ','LFL',' L '],{'L':'minecraft:lapis_block','F':'overload_sim:simulation_frame'},1),
 'simulation_overload_module':([' N ','MFM',' N '],{'N':'minecraft:netherite_ingot','M':'ae2lt:lightning_collapse_matrix','F':'overload_sim:simulation_frame'},1),
 'simulation_smelting_module':([' B ','BFB',' B '],{'B':'minecraft:blast_furnace','F':'overload_sim:simulation_frame'},1)}
for id,(pattern,key,count) in recipes.items():
 write(Path('data/overload_sim/recipe')/(id+'.json'),{'type':'minecraft:crafting_shaped','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':'overload_sim:'+id,'count':count}})
for locale,index in [('zh_cn',0),('en_us',1)]:
 path=RES/'assets/overload_sim/lang'/f'{locale}.json';lang=json.loads(path.read_text(encoding='utf8'))
 lang.update({'block.overload_sim.'+id:label[index] for id,label in NAMES.items()})
 lang.update({
 'gui.overload_sim.multiblock.crystals':['水晶：%s / %s','Crystals: %s / %s'][index],
 'gui.overload_sim.multiblock.outputs':['产物（第 %s / 4 页）','Outputs (page %s / 4)'][index],
 'gui.overload_sim.multiblock.take':['左键取一组，右键取一个，Shift取入背包','Left: one stack. Right: one item. Shift: fill inventory.'][index],
 'status.overload_sim.8':['结构所在区块未加载','Structure chunks unloaded'][index],
 'gui.overload_sim.multiblock.error':['结构诊断：%s','Structure diagnostic: %s'][index]})
 path.write_text(json.dumps(lang,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
header='---\nnavigation:\n  title: 多方块模拟室\n  icon: overload_sim:simulation_controller\n  parent: overload_sim:overload-simulation.md\nitem_ids:\n'+''.join('  - overload_sim:'+id+'\n' for id in NAMES)+'---\n\n'
guide=header+'''# 多方块模拟室

外部尺寸为 3×3×3 至 7×7×7。水晶容量依次为 9、16、25、36、49，存放在控制器中；内部空间必须为空气。

12 条边框使用模拟室框架。底部非角边框处放置**一个控制器**，使正面朝外。可以在另一处底部非角边框替换为**一个原版过载 ME 接口**。顶面及四侧面中央使用 AE 聚能石英玻璃；底面中央用框架或升级模块填满。

控制器会自动检测并成型。右键打开界面；G 键查看本页。放入完美模拟电鸣水晶，接入有电且有高压闪电库存的 AE 网络即可生产。

## 生产与升级

每个参加生产的水晶每 180 tick 执行一次现有模拟配方，每次扣 1000 FE + 1 高压。费用按水晶计算。

|底部模块|效果|
|---|---|
|效率 T1 / T2 / T3|每个减少 2 / 4 / 8 tick，总共最多减 104 tick|
|时运|每个使一次实际产物 ×2；最多 ×1024（10 个）|
|过载（最多一个）|最终时间减半；每个水晶额外消耗 1 极高压，显示环绕电弧|
|熔炼（最多一个）|每个水晶额外消耗 2 高压；原铁/铜/金变成双倍锭，显示火焰；其他产物类型保持|

7 阶底面有 25 个升级位：13 个 T3 + 10 个时运 + 1 个过载 + 1 个熔炼，可达 38 tick 周期。49 个铁水晶可产 100352 个铁锭，费用 49000 FE、147 高压、49 极高压。

## 输出和拆解

输出缓冲有 128 格、四页，每格 1024 个同类同组件物品。左键取正常一组、右键取一个、Shift 将物品按正常堆叠填入背包。管道也只提取正常堆叠。

装有过载接口时，接口绑定的无线过载频道把整机接入网络，产物自动导入网络；不能装入的部分留在缓冲。没有接口时，可通过控制器连接 AE 线缆供电/取闪电，并用管道取出产物。

断电、区块卸载或拆掉结构会暂停已付款任务；恢复结构后继续，不重复扣费或抽取战利品。生存模式拆掉控制器会把库存、缓冲、能量和未完成任务保存在控制器物品里，重新放置后恢复。缩小结构后超出容量的水晶仍可取回。

## 分层搭建

F=框架；C=控制器（正面向北）；G=聚能石英玻璃；U=框架或底部模块；.=空气。I 过载接口可替换任意底部非角 F，不能替换 C。
'''
for n in range(3,8):
 bottom=['F'*n for _ in range(n)]
 bottom[0]='FC'+'F'*(n-2)
 for z in range(1,n-1):bottom[z]='F'+'U'*(n-2)+'F'
 middle=['F'+'G'*(n-2)+'F']+['G'+'.'*(n-2)+'G' for _ in range(n-2)]+['F'+'G'*(n-2)+'F']
 top=['F'*n]+['F'+'G'*(n-2)+'F' for _ in range(n-2)]+['F'*n]
 guide+=f'\n### {n}×{n}×{n}（{n*n} 水晶，{(n-2)**2} 升级位）\n'
 for label,rows in [('底层 y=0',bottom),(f'中间层 y=1…{n-2}',middle),(f'顶层 y={n-1}',top)]:
  guide+='\n'+label+'\n\n'+chr(96)*3+'\n'+'\n'.join(rows)+'\n'+chr(96)*3+'\n'
for folder in ['assets/overload_sim/ae2guide/_zh_cn','assets/overload_sim/ae2guide']:
 path=RES/folder/'multiblock-simulation.md';path.parent.mkdir(parents=True,exist_ok=True);path.write_text(guide,encoding='utf8')
print('Multiblock JSON, recipes, languages and five-size guide written; no pixel assets modified.')
