"""Generate laser recipes using the native AE2LT 2.1.0/2.1.1 recipe formats.

Materials and costs follow the user's 配方.txt. Dependency item IDs were checked
against both pinned JARs' Chinese translations and native recipe resources.
"""
import json
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
A, S, M = 'ae2lt:', 'overload_sim:', 'minecraft:'
H, E = 'high_voltage', 'extreme_high_voltage'
ASSEMBLY_RECIPES = [
    ('laser_optical_port',400_000,128,H,[(S+'laser_glass',8),(A+'firmament_alloy_ingot',8),(A+'ultimate_overload_core',1)]),
    ('high_energy_laser_emitter',1_000_000,128,E,[(S+'laser_frame',4),(A+'overload_processor',64),(A+'overload_alloy_plate',64),(A+'firmament_superconducting_wire',32),(A+'lightning_collapse_matrix',1)]),
    ('laser_constraint_matrix',400_000,32,E,[(S+'laser_frame',4),(A+'firmament_alloy_ingot',4),(A+'firmament_superconducting_wire',4)]),
    ('laser_enhancement_matrix',800_000,32,E,[(S+'laser_frame',4),(A+'ultimate_overload_core',4),(A+'firmament_superconducting_wire',4)]),
    ('laser_burst_matrix',2_000_000,128,E,[(S+'laser_frame',4),(A+'lightning_collapse_matrix',4),(A+'firmament_superconducting_wire',16)]),
    ('laser_focus_matrix',800_000,64,E,[(S+'laser_frame',8),(S+'laser_glass',8),(A+'overload_processor',32)]),
    ('laser_deflector_matrix',400_000,32,E,[(S+'laser_frame',8),(S+'laser_glass',32),(A+'overload_crystal_block',32)]),
    ('laser_source_crystal',2_000_000,256,E,[(S+'laser_frame',2),(A+'flawless_budding_overload_crystal',1),(A+'overload_crystal_block',32)]),
    ('collapse_core',2_000_000,256,E,[(S+'laser_frame',2),(M+'heavy_core',1),('ae2:singularity',512),(M+'netherite_block',64)]),
    # User clarified "苍穹超导合金" to mean firmament alloy ingots, not wire.
    ('particle_constrainer',2_000_000,256,E,[(S+'laser_frame',8),(M+'heavy_core',1),(A+'overload_processor',64),('ae2:dense_energy_cell',64),(A+'firmament_alloy_ingot',64)]),
    ('high_energy_workstation',2_000_000,256,E,[(A+'overload_processing_factory',64),(A+'lightning_collapse_matrix',16),(A+'overload_processor',64)]),
]

def counted_inputs(materials):
    return [{'ingredient':{'item':item},'count':count} for item,count in materials]

def recipe_data():
    recipes = {}
    for name, fe, lightning, tier, materials in ASSEMBLY_RECIPES:
        recipes[name] = {'type':A+'lightning_assembly','priority':0,'inputs':counted_inputs(materials),
                         'result':{'id':S+name,'count':1},'totalEnergy':fe,'lightningCost':lightning,'lightningTier':tier}
    recipes['laser_frame'] = {'type':A+'overload_processing','priority':0,
        'inputs':counted_inputs([(A+'overload_alloy_plate',8),(M+'netherite_ingot',2),(M+'iron_block',2)]),
        'inputFluid':{'id':M+'lava','amount':4000},'results':[{'id':S+'laser_frame','count':1}],
        'totalEnergy':100_000,'lightningCost':32,'lightningTier':H}
    recipes['laser_glass'] = {'type':M+'crafting_shaped','category':'misc','pattern':['GGG','GFG','GGG'],
        'key':{'G':{'item':'ae2:quartz_glass'},'F':{'item':S+'laser_frame'}},'result':{'id':S+'laser_glass','count':1}}
    return recipes

def write_recipes():
    directory = ROOT / 'src/main/resources/data/overload_sim/recipe'
    recipes = recipe_data()
    for name, recipe in recipes.items():
        (directory/(name+'.json')).write_text(json.dumps(recipe,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
    for name in ['high_energy_workstation_frame_to_laser_frame','high_energy_workstation_glass_to_laser_glass','high_energy_workstation_receiver_to_laser_optical_port']:
        (directory/(name+'.json')).unlink(missing_ok=True)
    print(f'{len(recipes)} laser manufacturing recipes written; obsolete recovery recipes removed.')

if __name__ == '__main__':
    write_recipes()
