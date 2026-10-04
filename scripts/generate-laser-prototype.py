"""Reproduce minimal voxel assets and a Blockbench workshop; reuse existing PNGs unchanged.

The workshop contains one group per exported block. No bitmap painting, texture
copying, central language changes or registry changes happen here.
"""
from pathlib import Path
import base64
import copy
import json
import struct
import uuid

ROOT = Path(__file__).resolve().parents[1]
ASSETS = ROOT / 'src/main/resources/assets/overload_sim'
DATA = ROOT / 'src/main/resources/data/overload_sim'
WORKSHOP = ROOT / 'art/blockbench'
MACHINES = ['high_energy_laser_emitter', 'laser_constraint_matrix', 'laser_enhancement_matrix',
            'laser_burst_matrix', 'laser_deflector_matrix', 'laser_focus_matrix', 'collapse_core', 'particle_constrainer']
PARTS = ['laser_frame', 'laser_glass', 'laser_source_crystal', 'laser_optical_port']
FACES = ('north', 'east', 'south', 'west', 'up', 'down')
TEXTURES = ['block/simulation_controller', 'block/simulation_controller_front',
            'block/simulation_frame', 'block/simulation_frame_formed',
            'block/simulation_chamber_glass', 'item/perfect_simulation_crystal']

def write(path, value):
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(json.dumps(value, ensure_ascii=False, indent=2) + '\n', encoding='utf-8')

def cube(name, start, end, texture='case', uv=None, front=None, cull=False):
    faces = {}
    for face in FACES:
        faces[face] = {'uv': uv or [0, 0, 16, 16], 'texture': '#' + (front if face == 'north' and front else texture)}
        if cull:
            faces[face]['cullface'] = face
    return {'name': name, 'from': start, 'to': end, 'faces': faces}

def make_model(elements, textures, translucent=False):
    return {'credit': 'Overload Simulation laser prototype; existing textures retain original licenses.',
            'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'render_type': 'minecraft:translucent' if translucent else 'minecraft:cutout',
            'texture_size': [32, 32],
            'textures': {'particle': 'minecraft:block/white_concrete', **textures}, 'elements': elements}

def crystal_elements():
    result = []
    for i, (bottom, top, width) in enumerate([(2, 4, 2), (4, 6, 4), (6, 10, 6), (10, 12, 4), (12, 14, 2)]):
        half = width // 2
        result.append(cube(f'crystal layer {i}', [8-half, bottom, 8-half], [8+half, top, 8+half], 'pink', [1, 1, 2, 2]))
    result.append(cube('crystal support', [4, 0, 4], [12, 2, 12], 'frame'))
    return result

models = {}
for index, name in enumerate(MACHINES):
    if name == 'collapse_core':
        elements = crystal_elements()
        for x in (2, 12):
            for z in (2, 12):
                elements.append(cube('core stabilizer', [x, 0, z], [x+2, 6, z+2], 'frame'))
        models[name] = make_model(elements, {'pink': 'overload_sim:block/simulation_frame',
                                           'frame': 'overload_sim:block/simulation_frame_formed'})
    else:
        elements = [cube('white housing and dark pink screen', [0, 0, .05], [16, 16, 16], front='screen', cull=True)]
        # Small physical markers distinguish the controller kinds without new painted textures.
        for marker in range(index + 1 if index < 6 else 3):
            x = 3 + marker * 1.5
            elements.append(cube(f'function marker {marker+1}', [x, 2, 0], [x+1, 3, .04], 'pink', [1, 1, 2, 2]))
        models[name] = make_model(elements, {'case': 'overload_sim:block/simulation_controller',
                                           'screen': 'overload_sim:block/simulation_controller_front',
                                           'pink': 'overload_sim:block/simulation_frame'})

models['laser_frame'] = make_model([cube('white pink structural frame', [0, 0, 0], [16, 16, 16], 'frame', cull=True)],
                                  {'frame': 'overload_sim:block/simulation_frame_formed'})
models['laser_glass'] = make_model([cube('transparent vanilla style glass', [0, 0, 0], [16, 16, 16], 'glass', cull=True)],
                                  {'glass': 'overload_sim:block/simulation_chamber_glass'}, True)
models['laser_source_crystal'] = make_model(crystal_elements(), {'pink': 'overload_sim:block/simulation_frame',
                                                              'frame': 'overload_sim:block/simulation_frame_formed'})
port = []
for x in (0, 13):
    for z in (0, 13):
        port.append(cube('vertical optical rail', [x, 0, z], [x+3, 16, z+3], 'frame'))
for y in (0, 13):
    for z in (0, 13):
        port.append(cube('horizontal optical rail', [3, y, z], [13, y+3, z+3], 'frame'))
    for x in (0, 13):
        port.append(cube('depth optical rail', [x, y, 3], [x+3, y+3, 13], 'frame'))
for axis in range(3):
    for edge in (1, 14):
        start, end = [3, 3, 3], [13, 13, 13]
        start[axis], end[axis] = edge, edge + 1
        port.append(cube('transparent optical window', start, end, 'glass'))
models['laser_optical_port'] = make_model(port, {'frame': 'overload_sim:block/simulation_frame_formed',
                                               'glass': 'overload_sim:block/simulation_chamber_glass'}, True)

rotation = {'north': {}, 'south': {'y': 180}, 'east': {'y': 90}, 'west': {'y': 270},
            'up': {'x': 270}, 'down': {'x': 90}}
for name, model in models.items():
    write(ASSETS / f'models/block/{name}.json', model)
    write(ASSETS / f'models/item/{name}.json', {'parent': f'overload_sim:block/{name}'})
    variants = {f'facing={face}': {'model': f'overload_sim:block/{name}', **turn} for face, turn in rotation.items()} if name in MACHINES else {'': {'model': f'overload_sim:block/{name}'}}
    write(ASSETS / f'blockstates/{name}.json', {'variants': variants})
    write(DATA / f'loot_table/blocks/{name}.json', {'type': 'minecraft:block', 'pools': [{
        'rolls': 1, 'entries': [{'type': 'minecraft:item', 'name': f'overload_sim:{name}'}],
        'conditions': [{'condition': 'minecraft:survives_explosion'}]}]})
write(DATA / 'tags/block/laser_mineable.json', {'replace': False, 'values': ['overload_sim:'+name for name in models]})
write(DATA / 'tags/block/laser_controllers.json', {'replace': False, 'values': ['overload_sim:'+name for name in MACHINES]})
write(DATA / 'tags/block/laser_parts.json', {'replace': False, 'values': ['overload_sim:'+name for name in PARTS]})

# Blockbench free-format workshop keeps the exact exported cubes with source PNGs embedded.
def stable_id(value):
    return str(uuid.uuid5(uuid.NAMESPACE_URL, 'overload_sim:laser-prototype/' + value))

textures = []
for i, texture in enumerate(TEXTURES):
    path = ASSETS / f'textures/{texture}.png'
    content = path.read_bytes()
    width, height = struct.unpack('>II', content[16:24])
    textures.append({'name': path.name, 'relative_path': '../../src/main/resources/assets/overload_sim/textures/'+texture+'.png',
                     'folder': texture.rsplit('/', 1)[0], 'namespace': 'overload_sim', 'id': str(i),
                     'width': width, 'height': height, 'uv_width': width, 'uv_height': height,
                     'particle': False, 'visible': True, 'internal': True, 'saved': True,
                     'uuid': stable_id(texture), 'source': 'data:image/png;base64,'+base64.b64encode(content).decode('ascii')})
elements, groups = [], []
for index, (name, model) in enumerate(models.items()):
    offset = [(index % 4)*24, (index // 4)*24, 0]
    group = {'name': name, 'origin': offset, 'uuid': stable_id(name), 'children': [], 'export': True,
             'visibility': True, 'isOpen': True, 'color': index % 8}
    for i, element in enumerate(model['elements']):
        element_id = stable_id(name+'/'+str(i))
        group['children'].append(element_id)
        item = copy.deepcopy(element)
        item.update({'type': 'cube', 'uuid': element_id, 'box_uv': False, 'autouv': 0,
                     'origin': offset, 'rotation': [0, 0, 0], 'export': True, 'visibility': True})
        for edge in ('from', 'to'):
            item[edge] = [element[edge][axis]+offset[axis] for axis in range(3)]
        for face in item['faces'].values():
            ref = model['textures'][face['texture'][1:]].split(':', 1)[1]
            texture_index = TEXTURES.index(ref)
            face['texture'] = texture_index
            scale = textures[texture_index]['uv_width'] / 16
            face['uv'] = [value * scale for value in face['uv']]
            face.pop('cullface', None)
        elements.append(item)
    groups.append(group)
write(WORKSHOP / 'laser-prototype.bbmodel', {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False},
      'name': 'Laser particle prototype', 'resolution': {'width': 32, 'height': 32},
      'elements': elements, 'outliner': groups, 'textures': textures,
      'credit': 'Overload Simulation; existing PNGs and licenses preserved. Regenerate with scripts/generate-laser-prototype.py.'})

names = {
    'high_energy_laser_emitter': ('高能激光发射器', 'High Energy Laser Emitter'),
    'laser_constraint_matrix': ('激光约束矩阵', 'Laser Constraint Matrix'),
    'laser_enhancement_matrix': ('激光强化矩阵', 'Laser Enhancement Matrix'),
    'laser_burst_matrix': ('激光爆发矩阵', 'Laser Burst Matrix'),
    'laser_deflector_matrix': ('激光偏折矩阵', 'Laser Deflector Matrix'),
    'laser_focus_matrix': ('激光聚焦矩阵', 'Laser Focus Matrix'),
    'collapse_core': ('坍缩核心', 'Collapse Core'),
    'particle_constrainer': ('粒子约束器', 'Particle Constrainer'),
    'laser_frame': ('激光框架', 'Laser Frame'), 'laser_glass': ('激光通光玻璃', 'Laser Glass'),
    'laser_source_crystal': ('激光源水晶', 'Laser Source Crystal'), 'laser_optical_port': ('激光光口', 'Laser Optical Port')}
strings = {
    'on': ('开启', 'On'), 'off': ('关闭', 'Off'), 'input': ('输入', 'Input'),
    'formed': ('结构已成型', 'Structure formed'), 'unformed': ('结构未成型', 'Structure incomplete'),
    'online': ('在线', 'Online'), 'offline': ('离线', 'Offline'),
    'network': ('AE 网络：%s', 'AE network: %s'), 'power': ('本批强度：%s FE', 'Batch power: %s FE'),
    'boost': ('强化增量：%s FE', 'Boost: +%s FE'),
    'boost_cost': ('每批支付与强化增量相同的 FE', 'Pays the selected boost each batch'),
    'outputs': ('输出面（输入面已锁定）', 'Output faces; input is locked'), 'side': ('%s：%s', '%s: %s'),
    'core_remainder': ('核心余量：%s / 512G FE', 'Remainder: %s / 512G FE'),
    'core_hint': ('需 ≥1G FE 聚焦束；每 512G 产一粒', 'Needs ≥1G FE; 512G FE per particle'),
    'forward_hint': ('粒子从核心另一侧继续向前射出', 'Particles leave the far side forward'),
    'particles': ('粒子库存：%s / 125000', 'Particles: %s / 125000'),
    'energy': ('FE 缓存：%s / 1024P', 'FE buffer: %s / 1024P'),
    'fuel': ('密封耗材栏', 'Sealed consumable input'), 'fuel_ticks': ('剩余 %s tick', '%s ticks left'),
    'burst_hint': ('每批强度 ×2，支付 64 极高压', 'Doubles the beam; pays 64 EHV'),
    'ehv': ('极高压：%s', 'EHV: %s'), 'focus_hint': ('在前方第 7 格聚焦至坍缩核心', 'Focuses on the core at block seven'),
    'range_hint': ('有效射程 6 格；按 20 tick 一批计量', 'Range: 6 blocks; 20 ticks per batch'),
    'sealed_fuel': ('投入后不可取出；无粒子或缓存满时暂停', 'Inserted fuel cannot be removed; pauses without particles or buffer space'),
    'active_fuel': ('使用中：%s · 粒子计时 %s tick', 'Active: %s · Particle timer: %s ticks'),
    'control_hint': ('成型后可开启；关闭立即停止光路', 'Enable after formation; disabling stops beams immediately'),
    'split_hint': ('输出强度平分；全关时停止入射', 'Splits power equally; all closed stops input'),
    'status': ('状态：%s', 'Status: %s'), 'exact_power': ('精确本批强度：%s FE', 'Exact batch power: %s FE'),
    'exact_energy': ('精确缓存：%s / %s FE', 'Exact buffer: %s / %s FE'),
    'guide_hint': ('G：激光与粒子搭建指南', 'G: laser and particle building guide')}
for face, values in {'north': ('北', 'N'), 'south': ('南', 'S'), 'east': ('东', 'E'), 'west': ('西', 'W'), 'up': ('上', 'Up'), 'down': ('下', 'Down')}.items():
    strings['face.'+face] = values
for status, values in {
    'no_structure': ('结构未成型', 'Structure incomplete'), 'idle': ('待机', 'Idle'),
    'working': ('有效光束运行中', 'Beam active'), 'converting': ('粒子转化中', 'Converting particles'),
    'no_energy': ('FE 或所需闪电不足', 'Insufficient FE or lightning'),
    'unloaded': ('结构所在区块未加载', 'Structure chunk unloaded'),
    'owned': ('结构与其它机器重叠', 'Structure overlaps another machine'),
    'controller': ('控制器无效', 'Invalid controller'), 'frame': ('缺少激光框架', 'Missing laser frame'),
    'glass': ('缺少通光玻璃', 'Missing laser glass'), 'source': ('缺少源水晶', 'Missing source crystal'),
    'port': ('缺少光口', 'Missing optical port'), 'air': ('内部或光孔被占用', 'Interior or aperture obstructed'),
    'interface_position': ('过载接口必须唯一且位于上游框架', 'Use one overload interface in an upstream frame'),
    'structure_changed': ('结构已改变，光路停止', 'Structure changed; beams stopped'),
    'particle_overflow': ('粒子库存过载', 'Particle containment overflow'),
    'invalid_loop_or_input': ('光路回环或重复入射', 'Loop or duplicate input'),
    'beam_limit': ('超过单束强度上限', 'Beam power limit exceeded'),
    'modifier_cancelled': ('矩阵操作被扩展事件取消', 'Modifier cancelled by extension'),
    'no_outputs': ('所有输出面已关闭', 'All output faces closed'),
    'fuel_rule_missing': ('已支付耗材规则不存在', 'Paid fuel rule unavailable'),
    'fuel_rule_changed': ('已支付耗材规则改变，处理中止', 'Paid fuel rule changed; processing paused'),
    'no_fuel': ('没有有效耗材', 'No valid consumable'), 'no_particles': ('没有粒子，计时暂停', 'No particles; clocks paused'),
    'energy_full': ('FE 缓存无法容纳下一批，计时暂停', 'No space for next FE yield; clocks paused'),
    'conversion_cancelled': ('粒子转化被扩展事件取消', 'Conversion cancelled by extension')}.items():
    strings['status.'+status] = values
for index, locale in enumerate(('zh_cn', 'en_us')):
    snippet = {'block.overload_sim.'+name: values[index] for name, values in names.items()}
    snippet.update({'gui.overload_sim.laser.'+key: values[index] for key, values in strings.items()})
    write(WORKSHOP / f'laser-lang-{locale}.json', snippet)

header = '''---
navigation:
  title: %s
  icon: overload_sim:high_energy_laser_emitter
  parent: overload_sim:overload-simulation.md
item_ids:
'''+''.join('  - overload_sim:'+name+'\n' for name in models)+'---\n\n'
zh = '''# 激光与粒子原型

沿光路依次搭建：**发射器 → 矩阵 → 聚焦矩阵 → 坍缩核心 → 粒子约束器**。粒子从核心的另一侧沿激光前进方向射出；约束器的接收面朝向核心，并在该侧七格以内。它不会挡在聚焦矩阵与核心之间。

控制器的屏幕朝上游，光路向屏幕反方向前进，支持六个空间方向。发射器为 5³ 外壳，中央放激光源水晶；普通矩阵为外径 7 格、厚 1 格的环，中心孔保持空气。偏折矩阵为 7³ 像素球壳，六面中央为光口。约束器为粉白笼架和通光玻璃。不同结构的表面之间至少空一格；控制器界面显示是否成型。每套结构可通过控制器接 AE 线缆，或使用一个过载 ME 接口接入过载网络。

### 环形矩阵正面（屏幕朝向玩家）

F 为激光框架，C 为对应矩阵控制器，点为空气。中心孔在控制器沿结构上方三格处；当光路竖直时，结构的“上方”为北。四种普通环使用相同模板。

```
..FFF..
.F...F.
F.....F
F.....F
F.....F
.F...F.
..FCF..
```

约束器为 7×7×7 的笼架：12 条边全部用激光框架，六个面内的其余格用通光玻璃，内部留空。一个控制器替换底面中央的玻璃，接收光口替换面向核心的面中央玻璃。核心应与该光口同轴。发射器控制器在背面中央，源水晶在向前两格的中心，出光口再向前两格。

发射器每 20 tick 支付 100000 FE，基础射程 6 格。约束矩阵支付 100000 FE 并重新给予 6 格射程；强化矩阵增加并支付界面所选的 100000～9000000 FE；爆发矩阵支付 64 极高压，使强度翻倍。偏折矩阵平分强度至选中的输出面，输入面不能选作输出，关闭全部输出会停机。单束强度上限为 1P FE。无有效入射的矩阵不扣运行费用。

聚焦矩阵在前方第 7 格汇聚到坍缩核心；聚焦途中清除可破坏方块，不可破坏方块阻断光路。核心只在达到 1G FE 时激活，累计每 512G FE 生成一粒子并保留余量。粉色圆形光束和逐渐收窄的聚焦束显示实际同步光路；蓝色点仅表现从核心向约束器运动的粒子，计数由服务器结算。

## 粒子与密封耗材

约束器容量为 125000 粒子，FE 缓存为 1024P FE。耗材投入后不能从界面或自动化抽出。每件耗材提供 100 个有效工作 tick；没有粒子或 FE 缓存无法容纳下一次产能时暂停，保留已支付耗材的剩余时间。

|耗材|每粒子间隔|每粒子产能|
|---|---|---|
|下界之星|100 tick|512G FE|
|下界合金块|20 tick|512G FE|
|奇点|10 tick|256G FE|
|过载奇点|2 tick|1024G FE|
|钚球 / 钋球（可选通用机械）|1 tick|2048G FE|
|反物质球（可选通用机械）|1 tick|4096G FE|

缓存只按 AE 网络实际接收量扣减。粒子库存超过上限，或拆除仍储有粒子的约束器结构，会触发以机器区块为中心的 3×3 已加载区块破坏；先关闭上游光路并排空库存再拆解。空约束器可正常调整。

右键控制器启停与调节，悬停状态区查看精确 long 计量、诊断原因和燃料计时。G 键可随时查看本页。创造管理员可在空区域使用 `/overload_sim laser build <kind>` 放置单个结构，或 `/overload_sim laser demo` 放置预充有限资源的完整链；演示包含强化、16 个爆发矩阵、约束补程、聚焦、核心和向前的接收器。模型为可继续细化的 Blockbench 粉白体素原型，贴图沿用现有资源。
'''
en = '''# Laser and particle prototype

Build the chain in this order: **emitter → matrices → focus matrix → collapse core → particle constrainer**. Particles leave the opposite side of the core, continuing in the laser's forward direction. Put the constrainer after the core, with its input facing the core and within seven blocks of that side.

The controller screen faces upstream; the beam travels in the opposite direction. All six directions work. The emitter uses a 5³ shell with a source crystal at its center. Ordinary matrices are seven blocks across and one block thick with an empty center. The deflector uses a 7³ voxel sphere with a central optical port on each face. The constrainer uses a white/pink cage and laser glass. Leave at least one empty block between structure surfaces. The screen reports formation. Connect the controller with AE cable, or place one Overloaded ME Interface in the structure for the overload network.

### Ring front view (screen facing you)

F is laser frame, C is the matrix controller, and dots are air. The center is three blocks along the structure's up direction from its controller. For a vertical beam, that direction is north. All four ordinary rings share this template.

```
..FFF..
.F...F.
F.....F
F.....F
F.....F
.F...F.
..FCF..
```

The receiver is a 7×7×7 cage: all twelve edges are laser frame, the other cells on all six faces are laser glass, and the interior is air. Replace the bottom face center with the controller and the face center toward the core with an optical port. Align the core with that port. The emitter controller sits at the back center, its source crystal is two blocks forward, and its output port is another two blocks forward.

The emitter pays 100000 FE every 20 ticks and starts with a six-block range. A constraint matrix pays 100000 FE and renews six blocks of range. An enhancement matrix adds and pays the selected 100000–9000000 FE. A burst matrix pays 64 EHV to double power. A deflector splits power among enabled output faces; its input face is locked, and closing every output stops incoming work. The maximum beam power is 1P FE. Matrices without valid incoming light pay no running cost.

A focus matrix converges on the collapse core at the seventh block ahead. It clears breakable cells before the target, while unbreakable blocks stop the beam. The core activates at 1G FE and creates one particle per accumulated 512G FE, keeping the remainder. Pink cylinders and narrowing cones show synchronized beam paths. Blue dots show particles travelling forward from the core to the receiver; the server owns the actual count.

## Particles and sealed consumables

The constrainer holds 125000 particles and 1024P FE. Inserted consumables cannot be removed through the screen or automation. Each consumable provides 100 effective working ticks. Processing pauses without particles or space for the next energy yield, preserving the paid window.

|Consumable|Interval per particle|Yield per particle|
|---|---|---|
|Nether Star|100 ticks|512G FE|
|Netherite Block|20 ticks|512G FE|
|Singularity|10 ticks|256G FE|
|Overloaded Singularity|2 ticks|1024G FE|
|Plutonium / Polonium pellet (optional Mekanism)|1 tick|2048G FE|
|Antimatter pellet (optional Mekanism)|1 tick|4096G FE|

The FE buffer debits only energy accepted by the AE network. Particle overflow, or dismantling a constrainer structure containing particles, triggers destruction in the loaded 3×3 chunk area centered on the machine. Stop upstream beams and drain particles before dismantling. Empty constrainers can be adjusted normally.

Right-click a controller for power and controls. Hover over the status area for exact long values, diagnostics and consumable timers. Press G to open this page. Creative administrators can use `/overload_sim laser build <kind>` in an empty area to place one structure, or `/overload_sim laser demo` for a complete chain precharged with finite resources. The demo uses enhancement, 16 burst matrices, range constraints, focus, the core and a forward receiver. The white/pink Blockbench voxel models are editable placeholders using existing textures.
'''
for folder, title, body in [('ae2guide', 'Laser and particles', en), ('ae2guide/_zh_cn', '激光与粒子', zh)]:
    path = ASSETS / folder / 'laser-particle.md'
    path.parent.mkdir(parents=True, exist_ok=True)
    path.write_text(header % title + body, encoding='utf-8')

readme = '''# Laser prototype Blockbench workshop

Open `laser-prototype.bbmodel` in Blockbench. Each named group is the geometry of
one matching `assets/overload_sim/models/block/<group>.json` export. Groups are
spaced apart for editing; they are individual blocks, not a multiblock blueprint.
The existing perfect crystal item and animated energy orb are rendered in-world
by the block entity renderer. The core orb is three blocks across when active.

Run `python scripts/generate-laser-prototype.py` from the repository to reproduce
the workshop, voxel models, blockstates, item models, loot tables, tags, guide
pages and language snippets. Run `python scripts/verify-laser-resources.py` to
check their parity and references. Existing PNGs are embedded without changes;
there are no new painted bitmap assets. The scripts do not change registries,
central language files, recipes, or existing tags. Merge the two language JSON
snippets into the central language files; append `#overload_sim:laser_mineable`
to Minecraft's `mineable/pickaxe` block tag during integration.

Texture attribution and licenses remain in `LICENSES.md`, `THIRD_PARTY_NOTICES.md`
and `art/licenses/AE2LT-LICENSE_ASSETS.md`. Reusing these textures does not change
their license. White concrete is used for runtime destruction particles. Glass
uses the existing translucent glass texture. All controller models support six
facing directions. Formed/active properties, if added, can reuse these variants.
'''
(WORKSHOP / 'laser-prototype-README.md').write_text(readme, encoding='utf-8')
print(f'Generated {len(models)} laser voxel block exports and Blockbench workshop; existing PNG files unchanged.')
