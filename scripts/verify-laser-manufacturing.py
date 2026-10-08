"""Check the requested laser manufacturing costs, counts and native recipe schemas."""
import json
import runpy
import zipfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
DIRECTORY = ROOT / 'src/main/resources/data/overload_sim/recipe'
A, S, M = 'ae2lt:', 'overload_sim:', 'minecraft:'
H, E = 'high_voltage', 'extreme_high_voltage'
# These expectations are transcribed from the user's 配方.txt, independently of generation.
ASSEMBLY = {
    'laser_optical_port': (400_000, 128, H, [(S+'laser_glass',8),(A+'firmament_alloy_ingot',8),(A+'ultimate_overload_core',1)]),
    'high_energy_laser_emitter': (1_000_000,128,E,[(S+'laser_frame',4),(A+'overload_processor',64),(A+'overload_alloy_plate',64),(A+'firmament_superconducting_wire',32),(A+'lightning_collapse_matrix',1)]),
    'laser_constraint_matrix': (400_000,32,E,[(S+'laser_frame',4),(A+'firmament_alloy_ingot',4),(A+'firmament_superconducting_wire',4)]),
    'laser_enhancement_matrix': (800_000,32,E,[(S+'laser_frame',4),(A+'ultimate_overload_core',4),(A+'firmament_superconducting_wire',4)]),
    'laser_burst_matrix': (2_000_000,128,E,[(S+'laser_frame',4),(A+'lightning_collapse_matrix',4),(A+'firmament_superconducting_wire',16)]),
    'laser_focus_matrix': (800_000,64,E,[(S+'laser_frame',8),(S+'laser_glass',8),(A+'overload_processor',32)]),
    'laser_deflector_matrix': (400_000,32,E,[(S+'laser_frame',8),(S+'laser_glass',32),(A+'overload_crystal_block',32)]),
    'laser_source_crystal': (2_000_000,256,E,[(S+'laser_frame',2),(A+'flawless_budding_overload_crystal',1),(A+'overload_crystal_block',32)]),
    'collapse_core': (2_000_000,256,E,[(S+'laser_frame',2),(M+'heavy_core',1),('ae2:singularity',512),(M+'netherite_block',64)]),
    'particle_constrainer': (2_000_000,256,E,[(S+'laser_frame',8),(M+'heavy_core',1),(A+'overload_processor',64),('ae2:dense_energy_cell',64),(A+'firmament_alloy_ingot',64)]),
    'high_energy_workstation': (2_000_000,256,E,[(A+'overload_processing_factory',64),(A+'lightning_collapse_matrix',16),(A+'overload_processor',64)]),
}

def verify():
    errors = []
    def check(name, expected):
        path = DIRECTORY / (name+'.json')
        if not path.exists():
            errors.append(f'{name}: missing recipe')
        elif json.loads(path.read_text(encoding='utf8')) != expected:
            errors.append(f'{name}: recipe differs from requested costs/materials/native schema')
    for name, (fe, lightning, tier, ingredients) in ASSEMBLY.items():
        check(name, {'type':A+'lightning_assembly','priority':0,
                     'inputs':[{'ingredient':{'item':item},'count':count} for item,count in ingredients],
                     'result':{'id':S+name,'count':1},'totalEnergy':fe,'lightningCost':lightning,'lightningTier':tier})
    check('laser_frame', {'type':A+'overload_processing','priority':0,
        'inputs':[{'ingredient':{'item':item},'count':count} for item,count in [(A+'overload_alloy_plate',8),(M+'netherite_ingot',2),(M+'iron_block',2)]],
        'inputFluid':{'id':M+'lava','amount':4000},'results':[{'id':S+'laser_frame','count':1}],
        'totalEnergy':100_000,'lightningCost':32,'lightningTier':H})
    check('laser_glass', {'type':M+'crafting_shaped','category':'misc','pattern':['GGG','GFG','GGG'],
        'key':{'G':{'item':'ae2:quartz_glass'},'F':{'item':S+'laser_frame'}},'result':{'id':S+'laser_glass','count':1}})
    for name in ['high_energy_workstation_frame_to_laser_frame','high_energy_workstation_glass_to_laser_glass','high_energy_workstation_receiver_to_laser_optical_port']:
        if (DIRECTORY/(name+'.json')).exists(): errors.append(f'{name}: removed legacy recovery recipe is still present')
    # Keep the earlier manufacturing graph intact while adding this independent batch.
    legacy = runpy.run_path(str(ROOT/'scripts/manufacturing_recipes.py'))
    for name, fe, lightning, tier, materials in legacy['RECIPES']:
        check(name, {'type':A+('lightning_simulation' if name=='blank_simulation_crystal' else 'lightning_assembly'),'priority':0,
                     'inputs':[{'ingredient':item if isinstance(item,dict) else {'item':item},'count':count} for item,count in materials],
                     'result':{'id':S+name,'count':1},'totalEnergy':fe,'lightningCost':lightning,'lightningTier':tier})
    translations = {
        'item.ae2lt.firmament_alloy_ingot':'苍穹合金锭',
        'item.ae2lt.firmament_superconducting_wire':'苍穹超导丝',
        'block.ae2lt.flawless_budding_overload_crystal':'无瑕的过载水晶母岩',
        'block.ae2lt.overload_processing_factory':'过载处理工厂',
    }
    for version in ['2.1.0','2.1.1']:
        with zipfile.ZipFile(ROOT/'libs'/f'ae2lt-{version}.jar') as dependency:
            language = json.loads(dependency.read('assets/ae2lt/lang/zh_cn.json'))
            for key, expected in translations.items():
                if language.get(key)!=expected: errors.append(f'AE2LT {version}: unexpected translation for {key}')
    for version in ['19.2.17','19.2.18']:
        with zipfile.ZipFile(ROOT/'libs'/f'appliedenergistics2-{version}.jar') as dependency:
            language = json.loads(dependency.read('assets/ae2/lang/zh_cn.json'))
            for key, expected in {'block.ae2.quartz_glass':'石英玻璃','block.ae2.dense_energy_cell':'致密能源元件'}.items():
                if language.get(key)!=expected: errors.append(f'AE2 {version}: unexpected translation for {key}')
    if errors: raise SystemExit('\n'.join(errors))
    print(f'PASS: {len(ASSEMBLY)+2} laser recipes, 18 preserved manufacturing recipes, deleted recovery recipes and pinned JAR material mappings')

if __name__ == '__main__':
    verify()
