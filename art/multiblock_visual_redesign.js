// Executed inside Blockbench desktop. Canvas painting, editable cubes and Java exports share one source.
globalThis.drawMultiblockSimulation=async function(){
    const ROOT='D:/MinecraftDev/OverloadSimulation';
    const write=(p,v)=>Blockbench.writeFile(ROOT+'/'+p,{savetype:'text',content:typeof v==='string'?v:JSON.stringify(v,null,2)});
    const png=(name,c)=>Blockbench.writeFile(ROOT+'/src/main/resources/assets/overload_sim/textures/block/'+name+'.png',{savetype:'image',content:c.toDataURL()});
    const pause=()=>new Promise(r=>setTimeout(r,120));
    const canvas=(w=32,h=w)=>{const c=document.createElement('canvas');c.width=w;c.height=h;c.getContext('2d').imageSmoothingEnabled=false;return c;};
    const base=(g)=>{g.fillStyle='#eeeaf2';g.fillRect(0,0,32,32);g.fillStyle='#fffafd';g.fillRect(0,0,32,3);g.fillStyle='#dfd9e7';g.fillRect(0,29,32,3);};
    function tile(kind){
        const c=canvas(),g=c.getContext('2d');base(g);
        if(kind==='roof'){g.fillStyle='#eeeaf2';g.fillRect(0,0,32,32);return c;}
        if(kind==='plain'){g.fillStyle='#e6e0eb';g.fillRect(0,15,32,1);return c;}
        if(kind==='casing'){
            g.fillStyle='#8e8199';g.fillRect(2,2,28,28);g.fillStyle='#f8f2fa';g.fillRect(3,3,26,26);
            g.fillStyle='#e9a4ce';g.fillRect(5,5,22,22);g.fillStyle='#fdf6fc';g.fillRect(6,6,20,20);
            g.fillStyle='#c9bdce';g.fillRect(9,9,14,14);g.fillStyle='#e8e0ed';g.fillRect(10,10,12,12);
            for(const [x,y]of [[2,2],[27,2],[2,27],[27,27]]){g.fillStyle='#ad8dAB';g.fillRect(x,y,3,3);g.fillStyle='#ffdaef';g.fillRect(x+1,y+1,1,1);}return c;
        }
        if(kind==='rim'||kind==='roofCorner'){
            g.fillStyle='#aa9bb2';g.fillRect(0,0,32,4);g.fillStyle='#f0aed3';g.fillRect(0,4,32,2);g.fillStyle='#fff3fb';g.fillRect(0,6,32,1);
            if(kind==='roofCorner'){g.fillStyle='#aa9bb2';g.fillRect(0,0,4,32);g.fillStyle='#f0aed3';g.fillRect(4,0,2,32);g.fillStyle='#fff3fb';g.fillRect(6,0,1,32);}return c;
        }
        if(kind==='corner'){
            // One elbow, not crossing rails: right horizontal beam joins the bottom vertical column.
            g.fillStyle='#eeeaf2';g.fillRect(0,0,32,32);
            g.fillStyle='#b7aabd';g.fillRect(7,7,18,25);g.fillRect(7,7,25,18);
            g.fillStyle='#d9cfdf';g.fillRect(9,9,14,23);g.fillRect(9,9,23,14);
            g.fillStyle='#edacd3';g.fillRect(4,4,28,2);g.fillRect(4,4,2,28);g.fillRect(26,26,6,2);g.fillRect(26,26,2,6);
            g.fillStyle='#fff2fb';g.fillRect(6,6,26,1);g.fillRect(6,6,1,26);g.fillRect(25,25,7,1);g.fillRect(25,25,1,7);
            // Joint plate, recessed pink pin and two bright pixels echo the Tianshu corner without repeating a frame.
            g.fillStyle='#8e8199';g.fillRect(10,10,11,11);g.fillStyle='#f8f2fa';g.fillRect(11,11,9,9);g.fillStyle='#d99ebf';g.fillRect(13,13,5,5);g.fillStyle='#ffe6f5';g.fillRect(13,13,3,1);
            // Copy the exact incoming beam-edge shading; do not add borders on disconnected edges.
            const h=tile('horizontal'),v=canvas(),vg=v.getContext('2d');vg.translate(32,0);vg.rotate(Math.PI/2);vg.drawImage(h,0,0);
            g.drawImage(h,31,0,1,32,31,0,1,32);g.drawImage(v,0,31,32,1,0,31,32,1);
            return c;
        }
        // Uniform ends let horizontal and rotated vertical beams share the same corner pixel.
        g.fillStyle='#eeeaf2';g.fillRect(0,0,32,32);
        g.fillStyle='#b7aabd';g.fillRect(0,7,32,18);g.fillStyle='#d9cfdf';g.fillRect(0,9,32,14);
        g.fillStyle='#edacd3';g.fillRect(0,4,32,2);g.fillRect(0,26,32,2);g.fillStyle='#fff2fb';g.fillRect(0,6,32,1);g.fillRect(0,25,32,1);
        if(kind==='controller'||kind==='standaloneController'){
            g.fillStyle='#b8749e';g.fillRect(5,7,22,19);g.fillStyle='#ffc9e8';g.fillRect(6,8,20,17);g.fillStyle='#15121c';g.fillRect(8,10,16,13);
            g.fillStyle='#402d42';g.fillRect(9,11,14,1);g.fillStyle='#704761';g.fillRect(10,20,12,1);
        }
        if(kind==='port'){
            g.fillStyle='#b8749e';g.fillRect(8,8,16,16);g.fillStyle='#ffdef1';g.fillRect(9,9,14,14);g.fillStyle='#83768e';g.fillRect(11,11,10,10);
            g.fillStyle='#f9bbdf';g.fillRect(13,13,6,6);g.fillStyle='#fff2fb';g.fillRect(15,11,2,10);g.fillRect(11,15,10,2);
        }return c;
    }
    const body=tile('casing'),front=tile('standaloneController');
    const shell=canvas(128),sg=shell.getContext('2d');
    const tileKinds=['plain','horizontal','vertical','casing','controller','port','roof','corner','rim','roofCorner','plain','plain','plain','plain','plain','plain'];
    tileKinds.forEach((kind,i)=>{const c=kind==='vertical'?tile('horizontal'):tile(kind);sg.save();sg.translate(i%4*32,Math.floor(i/4)*32);if(kind==='vertical'){sg.translate(32,0);sg.rotate(Math.PI/2);}sg.drawImage(c,0,0);sg.restore();});
    function glass(){const c=canvas(16),g=c.getContext('2d');g.fillStyle='rgba(248,224,239,.13)';g.fillRect(0,0,16,16);return c;}
    function flow(orange){
        const c=canvas(32,512),g=c.getContext('2d'),colors=orange?['#66321f','#c85424','#f3a84b','#fff1b6']:['#572444','#b74883','#f69ac9','#fff0fb'];
        for(let f=0;f<16;f++)for(let y=0;y<32;y++)for(let x=0;x<32;x++){
            const band=Math.sin(x*.43+Math.sin(y*.32+f*.32)*2.2-f*.52)+Math.sin(y*.35-x*.16+f*.42);
            const sparkle=(x*17+y*31+f*3)%53===0;g.fillStyle=colors[sparkle?3:band>.9?2:band>-.8?1:0];g.fillRect(x,y+f*32,1,1);
        }return c;
    }
    const pink=flow(false),orange=flow(true),ecg=canvas(32,256),eg=ecg.getContext('2d');
    for(let f=0;f<16;f++){
        eg.fillStyle='#14121b';eg.fillRect(0,f*16,32,16);eg.fillStyle='#362336';eg.fillRect(0,f*16+8,32,1);
        for(let x=0;x<32;x++){let k=(x+f*2)%32,y=k<10||k>22?8:k===10?9:k===11?11:k<15?Math.max(1,11-(k-11)*4):k<18?Math.min(13,1+(k-14)*4):k===18?11:k===19?7:k===20?6:8;eg.fillStyle='#ffa5d8';eg.fillRect(x,f*16+y,1,1);if(x%5===0){eg.fillStyle='#ffeafa';eg.fillRect(x,f*16+y,1,1);}}
    }
    function project(name,w=32,h=w){newProject(Formats.java_block);Project.name=name;Project.texture_width=w;Project.texture_height=h;Project.ambientocclusion=false;}
    async function texture(name,c,w=c.width,h=c.height,layer='原创粉白像素图层'){
        const t=new Texture({name:name+'.png',namespace:'overload_sim',folder:'block',width:c.width,height:c.height,uv_width:w,uv_height:h}).fromDataURL(c.toDataURL()).add();await pause();
        t.layers_enabled=true;t.layers.length=0;t.layers.push(new TextureLayer({name:layer,width:c.width,height:c.height,image_data:c.getContext('2d').getImageData(0,0,c.width,c.height),opacity:100,visible:true,blend_mode:'default'},t));t.selected_layer=t.layers[0];t.updateLayerChanges(true);
        t.path=ROOT+'/src/main/resources/assets/overload_sim/textures/block/'+name+'.png';png(name,c);t.saved=true;return t;
    }
    function box(group,name,from,to,t,w=32,h=w,cull=false){
        const cube=new Cube({name,from,to,box_uv:false}).init();if(group)cube.addTo(group);
        for(const [side,f]of Object.entries(cube.faces)){f.texture=t.uuid;f.uv=[0,0,w,h];f.cullface=cull?side:'';}return cube;
    }
    async function save(name,render='minecraft:cutout'){
        Canvas.updateAll();const model=JSON.parse(Codecs.java_block.compile({raw:false}));model.parent='minecraft:block/block';model.render_type=render;model.ambientocclusion=false;
        model.textures.particle='overload_sim:block/simulation_frame_formed';
        write('src/main/resources/assets/overload_sim/models/block/'+name+'.json',model);write('art/'+name+'.bbmodel',Codecs.project.compile());Project.save_path=ROOT+'/art/'+name+'.bbmodel';Project.saved=true;return model;
    }
    const ids=['simulation_controller','simulation_frame','simulation_efficiency_t1','simulation_efficiency_t2','simulation_efficiency_t3','simulation_fortune_module','simulation_overload_module','simulation_smelting_module'];
    for(let i=0;i<ids.length;i++){
        const id=ids[i];project(id);const c=canvas(),g=c.getContext('2d');g.drawImage(body,0,0);
        if(i>1){g.fillStyle=i===7?'#ffb56f':i===5?'#f5d09f':'#f6b1d8';g.fillRect(10,10,12,12);g.fillStyle='#fff5fc';if(i<5)for(let k=0;k<i-1;k++)g.fillRect(11+k*3,12,2,8);else{g.fillRect(12,12,8,2);g.fillRect(12,18,8,2);g.fillRect(12,14,2,4);g.fillRect(18,14,2,4);}}
        const t=await texture(id,c);const cube=box(new Group({name:'天枢风格白色外壳与粉色细框'}).init(),'外壳',[0,0,0],[16,16,16],t,32,32,true);
        if(i===0){const screen=await texture('simulation_controller_front',front);cube.faces.north.texture=screen.uuid;}
        await save(id);write('src/main/resources/assets/overload_sim/models/item/'+id+'.json',{parent:'overload_sim:block/'+id});
        if(i===0)write('src/main/resources/assets/overload_sim/blockstates/'+id+'.json',{variants:{'facing=north':{model:'overload_sim:block/'+id},'facing=east':{model:'overload_sim:block/'+id,y:90},'facing=south':{model:'overload_sim:block/'+id,y:180},'facing=west':{model:'overload_sim:block/'+id,y:270}}});
        else write('src/main/resources/assets/overload_sim/blockstates/'+id+'.json',{variants:{'formed=false':{model:'overload_sim:block/'+id},'formed=true':{model:'overload_sim:block/'+(i===1?'simulation_frame_formed':id)}}});
    }
    project('simulation_frame_formed');var formed=await texture('simulation_frame_formed',tile('plain'));box(new Group({name:'连接模型加载前的白色回退面'}).init(),'白色面',[0,0,0],[16,16,16],formed,32,32,true);await save('simulation_frame_formed');
    project('simulation_shell_atlas',128);var atlas=await texture('simulation_shell_atlas',shell,128,128,'横梁 · 竖柱 · 转角 · 顶盖 · 黑屏 · 过载接口');box(new Group({name:'连接外壳十种图块'}).init(),'图集预览',[0,0,0],[16,16,16],atlas,128);await save('simulation_shell_atlas');
    for(const face of ['north','south','east','west','up','down']){
        const name='simulation_glass_'+face;project(name,16);const t=await texture(name,glass(),16,16,'无描边透明粉白玻璃');const bounds={north:[[0,0,1.15],[16,16,1.2]],south:[[0,0,14.8],[16,16,14.85]],west:[[1.15,0,0],[1.2,16,16]],east:[[14.8,0,0],[14.85,16,16]],up:[[0,14.8,0],[16,14.85,16]],down:[[0,1.15,0],[16,1.2,16]]}[face];
        const cube=box(new Group({name:'无边框玻璃薄面'}).init(),'玻璃',...bounds,t,16);const sides=['north','south'].includes(face)?['north','south']:['west','east'].includes(face)?['west','east']:['up','down'];for(const [side,f]of Object.entries(cube.faces))if(!sides.includes(side))f.texture=null;await save(name,'minecraft:translucent');
    }
    function sphere(group,t,center=[8,8,8],scale=1){
        const range=(y,z)=>{let a=12,b=-1;for(let x=0;x<12;x++)if((x-5.5)**2+(y-5.5)**2+(z-5.5)**2<=35){a=Math.min(a,x);b=Math.max(b,x);}return [a,b+1];};
        for(let y=0;y<12;y++)for(let z=0;z<12;z++){
            const [a,b]=range(y,z);if(b<=a)continue;const from=[a+2,y+2,z+2],to=[b+2,y+3,z+3];
            const cube=box(group,'体素球截面 '+y+'-'+z,from.map((v,i)=>center[i]+(v-8)*scale),to.map((v,i)=>center[i]+(v-8)*scale),t);
            for(const [side,f]of Object.entries(cube.faces)){
                f.uv=side==='up'||side==='down'?[from[0]*2,from[2]*2,to[0]*2,to[2]*2]:side==='east'||side==='west'?[from[2]*2,(16-to[1])*2,to[2]*2,(16-from[1])*2]:[from[0]*2,(16-to[1])*2,to[0]*2,(16-from[1])*2];
                let neighbor=side==='up'?[y+1,z]:side==='down'?[y-1,z]:side==='north'?[y,z-1]:side==='south'?[y,z+1]:null;
                if(neighbor&&neighbor[0]>=0&&neighbor[0]<12&&neighbor[1]>=0&&neighbor[1]<12){const [na,nb]=range(...neighbor);if(na<=a&&nb>=b)f.texture=null;}
            }
        }
    }
    for(const [name,c]of [['simulation_energy_orb',pink],['simulation_energy_orb_orange',orange]]){
        project(name);const t=await texture(name,c,32,32,'像素能量流纹 · 16帧');t.frame_time=2;sphere(new Group({name:'真实体素球壳'}).init(),t);await save(name);
        write('src/main/resources/assets/overload_sim/textures/block/'+name+'.png.mcmeta',{animation:{width:32,height:32,frametime:2,interpolate:false}});
    }
    project('simulation_controller_ecg',32,16);const ECG=await texture('simulation_controller_ecg',ecg,32,16,'16帧粉白心电图');box(new Group({name:'独立屏幕动画层'}).init(),'心电图',[0,0,0],[16,8,0],ECG,32,16);await save('simulation_controller_ecg');write('src/main/resources/assets/overload_sim/textures/block/simulation_controller_ecg.png.mcmeta',{animation:{width:32,height:16,frametime:2,interpolate:false}});
    project('simulation_emitter');var winding=await texture('simulation_emitter',tile('casing'));const coil=new Group({name:'顶部粉白线圈'}).init();for(const [f,t]of [[[0,0,0],[16,3,3]],[[0,0,13],[16,3,16]],[[0,0,3],[3,3,13]],[[13,0,3],[16,3,13]]])box(coil,'悬浮绕组',f,t,winding);await save('simulation_emitter');
    project('multiblock_simulation_assembly');var frame=await texture('simulation_frame_formed',tile('plain')),pane=await texture('simulation_glass_north',glass(),16,16),core=await texture('simulation_energy_orb',pink,32,32,'能量球预览');
    const shellGroup=new Group({name:'连续粉白顶盖与边框'}).init();box(shellGroup,'一体顶盖',[0,12.8,0],[16,16,16],frame);box(shellGroup,'一体底板',[0,0,0],[16,3.2,16],frame);
    for(const [x,z]of [[0,0],[12.8,0],[0,12.8],[12.8,12.8]])box(shellGroup,'连续竖柱',[x,3.2,z],[x+3.2,12.8,z+3.2],frame);
    const panes=new Group({name:'四面无缝玻璃'}).init();for(const [f,t,sides]of [[[3.2,3.2,.3],[12.8,12.8,.35],['north','south']],[[3.2,3.2,15.65],[12.8,12.8,15.7],['north','south']],[[.3,3.2,3.2],[.35,12.8,12.8],['west','east']],[[15.65,3.2,3.2],[15.7,12.8,12.8],['west','east']]]){const c=box(panes,'整片内陷玻璃',f,t,pane,16);for(const [side,face]of Object.entries(c.faces))if(!sides.includes(side))face.texture=null;}
    sphere(new Group({name:'运行时能量球（预览占位）'}).init(),core,[8,8,8],.55);await save('multiblock_simulation_assembly');
    Preview.selected.loadAnglePreset({position:[32,24,40],target:[8,8,8],projection:'perspective'});Canvas.updateAll();await pause();Screencam.screenshotPreview(Preview.selected,{width:800,height:800},url=>Blockbench.writeFile(ROOT+'/art/multiblock_simulation_preview.png',{savetype:'image',content:url}));
    write('art/multiblock-export.json',{source:'Blockbench desktop canvas and Java model codec',visualVersion:11,playerBlocks:8,glassSides:4,shellAtlasTiles:16,orbFrames:16,ecgFrames:16,roof:'frame',preview:'multiblock_simulation_preview.png'});
    Blockbench.showQuickMessage('粉白连续外壳、无边玻璃、能量球及心电图已导出',6000);
};
