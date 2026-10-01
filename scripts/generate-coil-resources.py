"""Generate data/models/lang only. Bitmap and 3D tool art is authored in Blockbench."""
from pathlib import Path
import json

root=Path(__file__).resolve().parents[1]
assets=root/'src/main/resources/assets/overload_sim'
data=root/'src/main/resources/data/overload_sim'
def write(path,value):
    path.parent.mkdir(parents=True,exist_ok=True)
    path.write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
ids=['extreme_voltage_module','mimic_tool_module','ultimate_destruction_module','efficiency_module','fortune_module','silk_touch_module','wrench_module']
zh=['极高压模块','拟态工具模块','终极破坏模块','效率模块','时运模块','精准采集模块','扳手模块']
en=['Extreme Voltage Module','Mimic Tool Module','Ultimate Destruction Module','Efficiency Module','Fortune Module','Silk Touch Module','Wrench Module']
for item in ids:
    write(assets/f'models/item/{item}.json',{'parent':'minecraft:item/generated','textures':{'layer0':f'overload_sim:item/{item}'}})

common={
'tooltip.overload_sim.coil.controls':('短按松开：目标落雷 · 蓄力松开：劈自己','Tap and release: target strike · Charge and release: self strike'),
'tooltip.overload_sim.coil.config':('G：配置模块 · 潜行右键：土地工具用途','G: configure modules · Sneak-use: terrain tool action'),
'tooltip.overload_sim.coil.core_ready':('过载核心：已安装','Overload core: installed'),
'tooltip.overload_sim.coil.voltage':('每次落雷消耗：10 %s','Lightning cost: 10 %s'),
'tooltip.overload_sim.coil.module':('在过载装备工作站装入线圈 · 上限 1 个','Install in coil at Overload Device Workbench · Limit: 1'),
'tooltip.overload_sim.coil.requires_mimic':('前置：拟态工具模块','Requires: Mimic Tool Module'),
'tooltip.overload_sim.coil.extreme_voltage_module':('开启后用 10 极高压闪电生成自然雷','Enable to create natural lightning for 10 EHV'),
'tooltip.overload_sim.coil.mimic_tool_module':('下界合金等级的通用电流工具','Universal electric tool with netherite harvest tier'),
'tooltip.overload_sim.coil.ultimate_destruction_module':('突破采集等级，可破坏基岩与苍穹核心','Unrestricted harvest tier; breaks bedrock and firmament cores'),
'tooltip.overload_sim.coil.efficiency_module':('可调效率 0–10 级','Adjustable efficiency: 0–10'),
'tooltip.overload_sim.coil.fortune_module':('可调时运 0–5 级','Adjustable fortune: 0–5'),
'tooltip.overload_sim.coil.silk_touch_module':('精准采集开启时优先于时运','Silk Touch takes priority over Fortune when enabled'),
'message.overload_sim.coil.no_core':('需要安装过载核心','Install an Overload Core first'),
'message.overload_sim.coil.unbound':('绑定的工作站网络尚未在线','Bound workbench network is offline or unavailable'),
'message.overload_sim.coil.no_target':('未找到射程内的有效目标','No valid target in range'),
'message.overload_sim.coil.no_lightning':('网络内所选电压的闪电不足 10 个','The network needs at least 10 of the selected lightning tier'),
'message.overload_sim.coil.spawn_failed':('落雷被取消，已退还闪电','Lightning was canceled; payment refunded'),
'message.overload_sim.coil.self_ready':('蓄力完成：松开右键劈自己','Charge complete: release to strike yourself'),
'gui.overload_sim.coil.on':('开启','Enabled'),'gui.overload_sim.coil.off':('关闭','Disabled'),
'gui.overload_sim.coil.voltage':('落雷模式','Lightning mode'),
'gui.overload_sim.coil.efficiency':('效率模块','Efficiency module'),
'gui.overload_sim.coil.natural':('自然雷 · 10 极高压','Natural · 10 EHV'),
'gui.overload_sim.coil.artificial':('人工雷 · 10 高压','Artificial · 10 HV'),
'gui.overload_sim.coil.efficiency_level':('效率等级：%s / 10','Efficiency: %s / 10'),
'gui.overload_sim.coil.fortune_level':('时运等级：%s / 5','Fortune: %s / 5'),
'gui.overload_sim.coil.silk':('精准采集','Silk Touch'),
'gui.overload_sim.coil.natural_mode':('自然雷模式','Natural lightning'),
'gui.overload_sim.coil.wrench':('扳手模式','Wrench mode'),
'gui.overload_sim.coil.wrench_mode':('配置工具用途','Configurator mode'),
'gui.overload_sim.coil.no_mekanism':('未安装通用机械：仅 AE 扳手功能可用','Mekanism absent: AE wrench only'),
'tooltip.overload_sim.coil.wrench_module':('G 开启后禁用引雷，切换 AE 扳手与通用机械配置用途','Enable in G to disable lightning and use AE wrench / Mek configurator'),
'tooltip.overload_sim.coil.wrench_active':('扳手模式：引雷已关闭','Wrench mode: lightning disabled'),
'message.overload_sim.coil.disabled':('扳手模式开启：引雷已关闭','Wrench mode disables lightning'),
}
for mode,zhmode,enmode in zip(range(8),['通用扳手','物品','流体','化学品','能量','热量','清空','旋转'],['Wrench','Items','Fluids','Chemical','Energy','Heat','Empty','Rotate']):common[f'gui.overload_sim.coil.wrench_mode.{mode}']=(zhmode,enmode)
for index,lang in enumerate(['zh_cn','en_us']):
    path=assets/f'lang/{lang}.json';strings=json.loads(path.read_text(encoding='utf-8'))
    strings['item.overload_sim.resonance_coil']=['谐振雷鸣线圈','Resonance Lightning Coil'][index]
    for item,zhname,enname in zip(ids,zh,en):strings[f'item.overload_sim.{item}']=[zhname,enname][index]
    for key,values in common.items():strings[key]=values[index]
    write(path,strings)

def shaped(id,pattern,key):
    write(data/f'recipe/{id}.json',{'type':'minecraft:crafting_shaped','category':'equipment','pattern':pattern,'key':{k:{'item':v} for k,v in key.items()},'result':{'id':f'overload_sim:{id}','count':1}})
shaped('resonance_coil',['IFI','FCF',' R '],{'I':'minecraft:iron_ingot','F':'ae2:fluix_crystal','C':'overload_sim:perfect_simulation_crystal','R':'ae2:engineering_processor'})
materials={'extreme_voltage_module':'ae2lt:lightning_collapse_matrix','mimic_tool_module':'minecraft:netherite_ingot','ultimate_destruction_module':'ae2lt:ultimate_overload_core','efficiency_module':'minecraft:redstone_block','fortune_module':'minecraft:lapis_block','silk_touch_module':'minecraft:amethyst_block','wrench_module':'ae2:certus_quartz_wrench'}
for item,material in materials.items():shaped(item,['IFI','FCF','IRI'],{'I':'minecraft:iron_ingot','F':'ae2:fluix_crystal','C':material,'R':'ae2:engineering_processor'})

# Preserve the original model exported by Blockbench on future resource regeneration.
if not (assets/'models/item/resonance_coil.json').exists():
    write(assets/'models/item/resonance_coil.json',{'parent':'minecraft:item/handheld','textures':{'layer0':'overload_sim:item/resonance_coil_body'}})

print('Generated coil module models, recipes and Chinese/English language strings.')
