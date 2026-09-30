"""Prepare a scene in the ignored developer test world only."""
from pathlib import Path
import json
root=Path(__file__).resolve().parents[1]
path=root/'run-client/saves/ui-review/datapacks/visual-review/data/overload_sim_review/function/coil.mcfunction'
modules=['overload_sim:'+id for id in ['extreme_voltage_module','mimic_tool_module','ultimate_destruction_module','efficiency_module','fortune_module','silk_touch_module']]+['ae2lt:energy_module_t3']
def nbt(value):
    if isinstance(value,dict):return '{'+','.join(json.dumps(k)+':'+nbt(v) for k,v in value.items())+'}'
    if isinstance(value,list):return '['+','.join(nbt(v) for v in value)+']'
    return json.dumps(value)
contents=[{'slot':i,'item':{'id':id,'count':1}} for i,id in enumerate(modules)]
coil='overload_sim:resonance_coil[overload_sim:coil_core='+nbt({'id':'ae2lt:ultimate_overload_core','count':1})+',overload_sim:coil_modules='+nbt(contents)+',overload_sim:coil_fe=20000000000L]'
path.write_text('\n'.join([
'time set day','weather clear','gamerule keepInventory true','gamemode survival @s',
'effect give @s minecraft:resistance 600 4 true','effect give @s minecraft:fire_resistance 600 0 true',
'fill 996 179 995 1006 179 1007 minecraft:smooth_quartz',
'setblock 1000 180 998 ae2lt:overload_device_workbench',
'setblock 999 180 998 ae2:creative_energy_cell','setblock 1001 180 998 ae2:drive',
'setblock 1003 180 1001 minecraft:diamond_ore','setblock 1004 180 1001 minecraft:bedrock',
'setblock 1003 180 1002 minecraft:oak_log','setblock 1004 180 1002 ae2lt:firmament_conversion_core',
'item replace entity @s weapon.mainhand with '+coil,
'tp @s 1000.6 180 1003.3 180 6'
])+'\n',encoding='utf-8')
print('Prepared ignored developer scene:',path)
