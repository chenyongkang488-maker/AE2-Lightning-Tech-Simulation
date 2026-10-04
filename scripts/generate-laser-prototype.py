"""Bind the native Blockbench pixel atlas to reproducible, editable laser models.

Paint the atlas with art/laser_art_workshop.js in Blockbench before running this.
Existing chamber/crystal PNGs remain unchanged. Registries and recipes are untouched.
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
TEXTURES = ['block/laser_pixel_atlas', 'block/simulation_controller_ecg',
            'block/simulation_glass_north', 'item/resonance_coil_body',
            'item/perfect_simulation_crystal']

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
    return {'credit': 'qiqi: powder-white laser artwork painted in Blockbench; reused crystals retain CC BY-NC-SA 3.0.',
            'parent': 'minecraft:block/block', 'ambientocclusion': False,
            'render_type': 'minecraft:translucent' if translucent else 'minecraft:cutout',
            'texture_size': [32, 32],
            'textures': {'particle': 'minecraft:block/white_concrete', **textures}, 'elements': elements}

def tile(index):
    x, y = index % 8 * 2, index // 8 * 2
    return [x, y, x+2, y+2]

def part(name, start, end, index=0):
    return cube(name, start, end, 'atlas', tile(index))

def socket_elements(core=False):
    result = [part('ceramic pedestal', [2, 0, 2], [14, 2, 14], 16),
              part('recessed capacitor', [4, 2, 4], [12, 3, 12], 3),
              part('pink central socket', [5, 3, 5], [11, 4, 11], 23)]
    for x in (2, 12):
        for z in (2, 12):
            result.append(part('floating crystal stabilizer', [x, 2, z], [x+2, 6, z+2], 2))
    if core:
        for y, low, high in [(6,3,13), (11,5,11)]:
            for z in (low, high-1):
                result.append(part('collapse field rail', [low,y,z], [high,y+1,z+1], 19))
            for x in (low,high-1):
                result.append(part('collapse field rail', [x,y,low+1], [x+1,y+1,high-1], 19))
    else:
        # Floating voxel crystal facets use the existing coil's original metal palette.
        for i, (bottom, top, width) in enumerate([(6,8,2), (8,10,4), (10,12,6), (12,14,4), (14,16,2)]):
            half=width/2
            result.append(cube(f'pink crystal facet {i}', [8-half,bottom,8-half], [8+half,top,8+half], 'metal', [8,0,12,4]))
    return result

models = {}
for index, name in enumerate(MACHINES):
    if name == 'collapse_core':
        models[name] = make_model(socket_elements(True), {'atlas':'overload_sim:block/laser_pixel_atlas'})
    else:
        elements = [part('bevelled ceramic body', [.5,.5,1], [15.5,15.5,15.5], 0),
                    part('recessed screen bezel', [2,2,.5], [14,14,1], 21),
                    part('equipment function glyph', [4,6,.35], [12,14,.5], 8+index),
                    cube('dynamic pink ECG', [4,3,.3], [12,5,.48], 'ecg', [0,0,16,16]),
                    part('status lamp', [2.5,3,.2], [3.5,4,.4], 22)]
        for x in (0,14):
            elements.append(part('pink conductor rail', [x,1,0], [x+2,15,1.5], 2))
            elements.append(part('white rear spine', [x,1,14], [x+2,15,16], 16))
        for y in (0,14):
            elements.append(part('ceramic edge brace', [2,y,0], [14,y+2,16], 16))
        for x in (0,15.6):
            elements.append(part('side carbon vent', [x,5,4], [x+.4,11,12], 18))
        models[name] = make_model(elements, {'atlas':'overload_sim:block/laser_pixel_atlas',
                                           'ecg':'overload_sim:block/simulation_controller_ecg'})

frame=cube('ceramic frame with continuous pink channels', [0,0,0], [16,16,16], 'atlas', tile(17), cull=True)
frame['faces']['up']['uv']=tile(16);frame['faces']['down']['uv']=tile(16)
models['laser_frame'] = make_model([frame], {'atlas':'overload_sim:block/laser_pixel_atlas'})
models['laser_glass'] = make_model([cube('borderless pale pink observation glass', [0,0,0], [16,16,16], 'glass', cull=True)],
                                  {'glass':'overload_sim:block/simulation_glass_north'}, True)
models['laser_source_crystal'] = make_model(socket_elements(), {'atlas':'overload_sim:block/laser_pixel_atlas',
                                                              'metal':'overload_sim:item/resonance_coil_body'})
port = []
for x in (0, 13):
    for z in (0, 13):
        port.append(part('ceramic optical rail', [x,0,z], [x+3,16,z+3], 2))
for y in (0, 13):
    for z in (0, 13):
        port.append(part('ceramic optical rail', [3,y,z], [13,y+3,z+3], 2))
    for x in (0, 13):
        port.append(part('ceramic optical rail', [x,y,3], [x+3,y+3,13], 2))
for axis in range(3):
    for edge in (1, 14):
        start, end = [3, 3, 3], [13, 13, 13]
        start[axis], end[axis] = edge, edge + 1
        port.append(cube('transparent optical window', start, end, 'glass'))
models['laser_optical_port'] = make_model(port, {'atlas':'overload_sim:block/laser_pixel_atlas',
                                               'glass':'overload_sim:block/simulation_glass_north'}, True)

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
    frame_width, frame_height=width,height
    animation=path.with_suffix('.png.mcmeta')
    if animation.exists():
        meta=json.loads(animation.read_text(encoding='utf-8'))['animation']
        frame_width=meta.get('width',width);frame_height=meta.get('height',frame_width)
    textures.append({'name': path.name, 'relative_path': '../../src/main/resources/assets/overload_sim/textures/'+texture+'.png',
                     'folder': texture.rsplit('/', 1)[0], 'namespace': 'overload_sim', 'id': str(i),
                     'width': width, 'height': height, 'uv_width': frame_width, 'uv_height': frame_height,
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
            scales = [textures[texture_index]['uv_width']/16, textures[texture_index]['uv_height']/16]*2
            face['uv'] = [value*scale for value,scale in zip(face['uv'],scales)]
            face.pop('cullface', None)
        elements.append(item)
    groups.append(group)
write(WORKSHOP / 'laser-prototype.bbmodel', {'meta': {'format_version': '5.0', 'model_format': 'free', 'box_uv': False},
      'name': 'Laser particle prototype', 'resolution': {'width': 32, 'height': 32},
      'elements': elements, 'outliner': groups, 'textures': textures,
      'credit': 'qiqi: laser pixel atlas painted in Blockbench; reused PNGs and licenses preserved. Regenerate with scripts/generate-laser-prototype.py.'})

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
    'receiver_rebuild_required': ('旧版约束器已安全暂停，请重建球形外壳', 'Legacy receiver safely paused; rebuild its spherical shell'),
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

控制器的屏幕朝上游，光路向屏幕反方向前进，支持六个空间方向。发射器为 5³ 外壳，中央放激光源水晶；普通矩阵为外径 7 格、厚 1 格的环，中心孔保持空气。偏折矩阵为 7³ 像素球壳，六面中央为光口。约束器为粉白环形骨架支撑的透明玻璃球壳。不同结构的表面之间至少空一格；控制器界面显示是否成型。每套结构可通过控制器接 AE 线缆，或使用一个过载 ME 接口接入过载网络。

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

约束器为外径 7 格的像素球壳。以球心为原点，x、y、z 均为 -3～3，距离平方 x²+y²+z² 在 7～12 的格子属于外壳；外壳中任一坐标为 0 的格子使用激光框架，形成三道交叉大圆，其余外壳用通光玻璃，其它格子保持空气。共需 40 个框架、56 个玻璃、1 个控制器和 1 个光口。控制器放在球体最下方中央，光口放在朝向核心的球面中央；竖直光路以北为结构上方。核心应与光口同轴。创造模式可用 `/overload_sim laser build receiver` 自动搭建。旧版方形笼架更新后安全暂停并保留库存，重建球壳后恢复。发射器控制器在背面中央，源水晶在向前两格的中心，出光口再向前两格。

发射器每 20 tick 支付 100000 FE，基础射程 6 格。约束矩阵支付 100000 FE 并重新给予 6 格射程；强化矩阵增加并支付界面所选的 100000～9000000 FE；爆发矩阵支付 64 极高压，使强度翻倍。偏折矩阵平分强度至选中的输出面，输入面不能选作输出，关闭全部输出会停机。单束强度上限为 1P FE。无有效入射的矩阵不扣运行费用。

聚焦矩阵在前方第 7 格汇聚到坍缩核心；聚焦途中清除光轴上的可破坏方块，不可破坏方块阻断光路。核心只在达到 1G FE 时激活，累计每 512G FE 生成一粒子并保留余量。普通粉色圆形光束直径约 3 格，聚焦束从同等宽度逐渐收窄到核心，生物伤害范围同步收束；蓝色点仅表现从核心向约束器运动的粒子，计数由服务器结算。

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

右键控制器启停与调节，悬停状态区查看精确 long 计量、诊断原因和燃料计时。G 键可随时查看本页。创造管理员可在空区域使用 `/overload_sim laser build <kind>` 放置单个结构，或 `/overload_sim laser demo` 放置预充有限资源的完整链；演示包含强化、16 个爆发矩阵、约束补程、聚焦、核心和向前的接收器。粉白像素面板、导光槽和八种设备标识在 Blockbench 绘制；黑色屏幕显示动态心电图，球壳使用无边框浅粉玻璃。
'''
en = '''# Laser and particle prototype

Build the chain in this order: **emitter → matrices → focus matrix → collapse core → particle constrainer**. Particles leave the opposite side of the core, continuing in the laser's forward direction. Put the constrainer after the core, with its input facing the core and within seven blocks of that side.

The controller screen faces upstream; the beam travels in the opposite direction. All six directions work. The emitter uses a 5³ shell with a source crystal at its center. Ordinary matrices are seven blocks across and one block thick with an empty center. The deflector uses a 7³ voxel sphere with a central optical port on each face. The constrainer uses three white/pink great-circle ribs supporting a transparent glass sphere. Leave at least one empty block between structure surfaces. The screen reports formation. Connect the controller with AE cable, or place one Overloaded ME Interface in the structure for the overload network.

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

The receiver is a voxel sphere seven blocks across. Relative to its center, x/y/z range from -3 to 3. Shell cells have squared distance x²+y²+z² from 7 through 12 inclusive. Shell cells with any coordinate equal to zero use laser frame (three intersecting great-circle ribs); remaining shell cells use glass, and every other cell is air. It uses 40 frames, 56 glass, one controller at the bottom pole and one optical port at the upstream pole. For vertical beams, the construction up direction is north. Align the core with the port. Creative administrators can build it with `/overload_sim laser build receiver`. A saved legacy cube safely pauses and retains inventory until rebuilt as this sphere. The emitter controller sits at the back center, its source crystal is two blocks forward, and its output port is another two blocks forward.

The emitter pays 100000 FE every 20 ticks and starts with a six-block range. A constraint matrix pays 100000 FE and renews six blocks of range. An enhancement matrix adds and pays the selected 100000–9000000 FE. A burst matrix pays 64 EHV to double power. A deflector splits power among enabled output faces; its input face is locked, and closing every output stops incoming work. The maximum beam power is 1P FE. Matrices without valid incoming light pay no running cost.

A focus matrix converges on the collapse core at the seventh block ahead. It clears breakable cells on the optical axis before the target, while unbreakable blocks stop the beam. The core activates at 1G FE and creates one particle per accumulated 512G FE, keeping the remainder. Ordinary pink beams are three blocks across; focus cones start at that width and contract toward the core. Entity damage follows the same width. Blue dots show particles travelling forward from the core to the receiver; the server owns the actual count.

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

Right-click a controller for power and controls. Hover over the status area for exact long values, diagnostics and consumable timers. Press G to open this page. Creative administrators can use `/overload_sim laser build <kind>` in an empty area to place one structure, or `/overload_sim laser demo` for a complete chain precharged with finite resources. The demo uses enhancement, 16 burst matrices, range constraints, focus, the core and a forward receiver. Powder-white panels, pink channels and eight equipment glyphs are painted in Blockbench; recessed screens reuse the animated ECG and the sphere uses borderless pale glass.
'''
zh += '\n## 自动搭建\n\n先放置并确定控制器朝向，打开控制器界面，点击左侧工具栏的「自动搭建」。生存模式先使用背包和副手材料，不足部分从控制器当前已连接的 AE 网络补齐；可直接接 AE 线缆。未连接时仍可使用背包。创造模式无需材料。只补放缺失部件，保留合法的过载接口；材料不足、区域受阻或未加载时不开始搭建。放置保护取消或取材发生变化时会退还本次材料，无法退入 AE/背包的部分掉落给玩家。重复搭建完整结构不收费。\n\n约束器成型后使用连续的曲面球壳和粉白圆环显示，实际仍按上述方块模板搭建和判定；破坏或卸载后恢复普通方块显示。\n'
en += "\n## Automatic construction\n\nPlace and orient a controller, open its screen, then click Automatic build in the left toolbar. Survival uses the main inventory and offhand first, then the controller's currently connected AE network; direct AE cabling works before formation. Backpack-only construction also works offline. Creative needs no materials. Only missing parts are placed and valid interfaces are preserved. Missing materials, obstructions or unloaded cells prevent construction. Cancelled placement or changed extraction refunds the withdrawn materials; anything rejected by both AE and the backpack is dropped for the player. Repeating a complete build is free.\n\nThe formed constrainer renders as a continuous curved glass sphere and pink/white ribs; validation still uses the voxel template above. Breaking or unloading restores normal construction blocks.\n"
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
check their parity and references. The new pixel atlas is painted in native
Blockbench by loading art/laser_art_workshop.js and using its Tools action.
laser-pixel-atlas.bbmodel preserves its editable layer. Existing PNGs are unchanged. The scripts do not change registries,
central language files, recipes, or existing tags. Merge the two language JSON
snippets into the central language files; append `#overload_sim:laser_mineable`
to Minecraft's `mineable/pickaxe` block tag during integration.

Texture attribution and licenses remain in `LICENSES.md`, `THIRD_PARTY_NOTICES.md`
and `art/licenses/AE2LT-LICENSE_ASSETS.md`. Reusing these textures does not change
their license. White concrete is used for runtime destruction particles. Glass
uses the existing borderless pale glass texture. All controller models support six
facing directions. Formed/active properties, if added, can reuse these variants.
'''
(WORKSHOP / 'laser-prototype-README.md').write_text(readme, encoding='utf-8')
print(f'Generated {len(models)} detailed laser block exports and Blockbench workshop; atlas painted in Blockbench.')
