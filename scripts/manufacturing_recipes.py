"""Authoritative addon manufacturing recipes for the pinned AE2LT 2.1.0 serializers."""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
H = 'high_voltage'
E = 'extreme_high_voltage'
A = 'ae2lt:'
S = 'overload_sim:'
M = 'minecraft:'
BOOK = {'type': S+'enchanted_book', 'enchantment': M+'silk_touch', 'min_level': 1}
# Result, FE, lightning amount, tier, counted ingredients. Each operation yields one item.
RECIPES = [
    ('blank_simulation_crystal',200_000,32,H,[(A+'electro_chime_crystal',1),(A+'overload_crystal',16),(M+'amethyst_shard',16)]),
    ('overload_simulation_chamber',600_000,128,H,[(A+'overload_machine_frame',1),(A+'overload_processor',16),(A+'overload_alloy_plate',16),(A+'ultimate_overload_core',4)]),
    ('resonance_coil',400_000,64,E,[(A+'lightning_collector',1),(M+'lightning_rod',1),(A+'ultimate_overload_core',4),(A+'overload_alloy_plate',32),(A+'overload_processor',4),(A+'overload_crystal',4)]),
    ('simulation_controller',400_000,64,E,[(S+'overload_simulation_chamber',1),(S+'simulation_frame',4),(A+'overload_processor',16)]),
    ('simulation_frame',40_000,32,H,[(A+'overload_alloy_plate',8),('ae2:quartz_vibrant_glass',8),(M+'quartz',8)]),
    ('simulation_efficiency_t1',100_000,32,E,[('ae2:speed_card',4),(A+'overload_crystal',4),(S+'simulation_frame',1)]),
    ('simulation_efficiency_t2',400_000,64,E,[(S+'simulation_efficiency_t1',4),(A+'overload_crystal_block',16),(M+'nether_star',1)]),
    ('simulation_efficiency_t3',1_600_000,128,E,[(S+'simulation_efficiency_t2',4),(A+'overload_singularity',32),(A+'lightning_collapse_matrix',1)]),
    ('simulation_fortune_module',1_600_000,256,E,[(A+'lightning_collapse_matrix',8),(A+'overload_singularity',64),(M+'dragon_egg',1),(A+'firmament_alloy_ingot',64),(S+'simulation_frame',1)]),
    ('simulation_overload_module',200_000,64,E,[(A+'lightning_collapse_matrix',1),(M+'netherite_block',16),(S+'simulation_frame',1)]),
    ('simulation_smelting_module',100_000,32,E,[(M+'blast_furnace',64),(S+'simulation_frame',1)]),
    ('extreme_voltage_module',200_000,64,E,[(A+'overload_module_base',1),(A+'lightning_collapse_matrix',1),(A+'tesla_coil',1),(A+'thunderstorm_condensate',1)]),
    ('mimic_tool_module',200_000,32,E,[(A+'overload_module_base',1),(M+'diamond',64),(M+'netherite_ingot',8),(A+'overload_processor',4)]),
    ('ultimate_destruction_module',800_000,256,E,[(A+'overload_module_base',1),(A+'lightning_collapse_matrix',1),(A+'overload_tnt',64),(M+'respawn_anchor',16),(M+'heart_of_the_sea',1)]),
    ('efficiency_module',800_000,256,E,[(A+'overload_module_base',1),(S+'simulation_efficiency_t3',4)]),
    ('fortune_module',800_000,256,E,[(A+'overload_module_base',1),(S+'simulation_fortune_module',4)]),
    ('silk_touch_module',100_000,128,H,[(A+'overload_module_base',1),(BOOK,1)]),
    ('wrench_module',100_000,128,H,[(A+'overload_module_base',1),('ae2:certus_quartz_wrench',1),(A+'overload_circuit_board',16)]),
]

def write_recipes():
    directory = ROOT/'src/main/resources/data/overload_sim/recipe'
    for name,fe,lightning,tier,inputs in RECIPES:
        value = {'type': A+('lightning_simulation' if name == 'blank_simulation_crystal' else 'lightning_assembly'), 'priority': 0,
                 'inputs': [{'ingredient': ingredient if isinstance(ingredient,dict) else {'item':ingredient}, 'count':count} for ingredient,count in inputs],
                 'result': {'id':S+name,'count':1}, 'totalEnergy':fe, 'lightningCost':lightning, 'lightningTier':tier}
        (directory/(name+'.json')).write_text(json.dumps(value,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    print(f'{len(RECIPES)} native lightning manufacturing recipes written.')

if __name__ == '__main__':
    write_recipes()
