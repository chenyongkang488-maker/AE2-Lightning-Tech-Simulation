"""Small reproducible empty GameTest bounds, not gameplay/world data."""
from pathlib import Path
import gzip,struct
def name(s):
    b=s.encode();return struct.pack('>H',len(b))+b
def tag(kind,key,data):return bytes([kind])+name(key)+data
def empty(x,y,z):
    return b'\x0a\x00\x00'+tag(3,'DataVersion',struct.pack('>i',3955))+tag(9,'size',b'\x03'+struct.pack('>iiii',3,x,y,z))+tag(9,'palette',b'\x0a'+struct.pack('>i',1)+tag(8,'Name',name('minecraft:air'))+b'\x00')+tag(9,'blocks',b'\x0a'+struct.pack('>i',0))+tag(9,'entities',b'\x0a'+struct.pack('>i',0))+b'\x00'
root=Path(__file__).resolve().parents[1]/'src/main/resources/data/overload_sim/structure'
for key,size in [('laser_empty',(32,12,32)),('laser_demo',(96,12,16))]:
    (root/f'{key}.nbt').write_bytes(gzip.compress(empty(*size),mtime=0))
