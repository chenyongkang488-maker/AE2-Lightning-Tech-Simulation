"""Original qiqi transition: descending charge, compressed impact, rising electric expansion.
Requires numpy and soundfile; produces genuine mono Vorbis, no sampled third-party audio.
"""
from pathlib import Path
import sys
sys.path.insert(0,str(Path(__file__).resolve().parents[1]/'build/audio-tools'))
import numpy as np
import soundfile as sf

root=Path(__file__).resolve().parents[1]
rate=44100
t=np.arange(rate*2)/rate
rng=np.random.default_rng(741)
charge=np.sin(2*np.pi*(740*t-280*t*t))*np.maximum(0,1-t/1.1)**.7
charge+=.25*np.sin(2*np.pi*(1100*t-370*t*t))*np.maximum(0,1-t)**.5
burst=np.exp(-((t-1.08)/.04)**2)*(rng.standard_normal(t.shape)*.22+np.sin(2*np.pi*64*t))
u=np.maximum(0,t-1.08)
expansion=np.sin(2*np.pi*(180*u+240*u*u))*np.where(t>1.08,np.exp(-u*3),0)
noise=rng.standard_normal(t.shape)
noise=np.convolve(noise,np.ones(5)/5,mode='same')
crackle=noise*np.where(t>.95,np.exp(-u*4),.03)*(1+np.sin(2*np.pi*29*t))*.24
signal=(charge*.15+burst*.3+expansion*.22+crackle)*np.minimum(1,t/.025)*np.minimum(1,(2-t)/.08)
assert np.isfinite(signal).all() and np.max(np.abs(signal))<1
target=root/'src/main/resources/assets/overload_sim/sounds/collapse_transition.ogg'
target.parent.mkdir(parents=True,exist_ok=True)
sf.write(target,signal,rate,format='OGG',subtype='VORBIS')
data,samplerate=sf.read(target)
assert samplerate==rate and len(data)==rate*2 and data.ndim==1
print(sf.info(target))
