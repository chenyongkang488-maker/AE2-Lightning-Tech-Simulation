"""Merge prototype language additions without changing existing gameplay IDs."""
import json
from pathlib import Path
root=Path(__file__).resolve().parents[1]
for locale in ('zh_cn','en_us'):
    path=root/f'src/main/resources/assets/overload_sim/lang/{locale}.json'
    data=json.loads(path.read_text(encoding='utf-8-sig'))
    data.update(json.loads((root/f'art/blockbench/laser-lang-{locale}.json').read_text(encoding='utf-8-sig')))
    statuses={'working':'运行中','idle':'待机','no_structure':'结构未成型','no_energy':'能量或极高压不足','unloaded':'结构区块未加载','owned':'结构被另一台设备占用','interface_position':'过载接口位置不正确','structure_changed':'结构已改变','beam_limit':'超过 1P 光束上限','modifier_cancelled':'矩阵事件已取消','invalid_loop_or_input':'光路循环或重复输入','no_outputs':'输出面已全部关闭','no_fuel':'等待耗材','no_particles':'等待粒子','energy_full':'FE 缓存已满','converting':'转化中','fuel_rule_missing':'已支付耗材的配方不存在','fuel_rule_changed':'已支付耗材的配方发生改变','conversion_cancelled':'粒子转化事件已取消','particle_overflow':'粒子库存溢出'}
    for key,value in statuses.items():data[f'gui.overload_sim.laser.status.{key}']=value if locale=='zh_cn' else key.replace('_',' ').capitalize()
    data['death.attack.overload_sim.high_energy_laser']='%1$s 被高能激光击穿' if locale=='zh_cn' else '%1$s was pierced by a high energy laser'
    path.write_text(json.dumps(data,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
path=root/'src/main/resources/data/minecraft/tags/block/mineable/pickaxe.json'
data=json.loads(path.read_text(encoding='utf-8-sig'))
if '#overload_sim:laser_mineable' not in data['values']:data['values'].append('#overload_sim:laser_mineable')
path.write_text(json.dumps(data,indent=2)+'\n',encoding='utf-8')
