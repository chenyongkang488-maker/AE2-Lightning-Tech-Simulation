// Run in Blockbench's desktop console. All raster painting and geometry use Blockbench's canvas/model APIs.
// Original pink-white artwork; no imported upstream block texture.
globalThis.drawMultiblockSimulation = async function(){
    const ROOT='D:/MinecraftDev/OverloadSimulation';
    const write=(p,v)=>Blockbench.writeFile(ROOT+'/'+p,{savetype:'text',content:typeof v==='string'?v:JSON.stringify(v,null,2)});
    const png=(p,c)=>Blockbench.writeFile(ROOT+'/'+p,{savetype:'image',content:c.toDataURL()});
    const pause=()=>new Promise(r=>setTimeout(r,180));
    const atlas=(glyph=0)=>{
        const c=document.createElement('canvas');c.width=c.height=32;const g=c.getContext('2d');g.imageSmoothingEnabled=false;
        // Seamless ceramic, windings, dark inset and module face.
        g.fillStyle='#dee0e8';g.fillRect(0,0,16,16);g.fillStyle='#f8f6fc';g.fillRect(0,1,16,3);
        g.fillStyle='#a7a8b9';g.fillRect(0,13,16,3);g.fillStyle='#ec97c3';g.fillRect(0,10,16,2);g.fillStyle='#ffe6f5';g.fillRect(0,10,16,1);
        g.fillStyle='#cd73a5';g.fillRect(16,0,16,16);for(let x=16;x<32;x+=3){g.fillStyle='#fff0f9';g.fillRect(x,0,1,16);g.fillStyle='#a95785';g.fillRect(x+1,0,1,16);}
        g.fillStyle='#545367';g.fillRect(0,16,16,16);g.fillStyle='#9394a9';g.fillRect(1,17,14,2);g.fillStyle='#343545';g.fillRect(2,21,12,9);
        g.fillStyle='#c9c9d6';g.fillRect(16,16,16,16);g.fillStyle='#fff6fc';g.fillRect(17,17,14,2);
        g.fillStyle='#79798e';g.fillRect(18,20,12,10);g.fillStyle=['#efa0cb','#b7d9f2','#d6b5ed','#f3c5f0','#efdd95','#ed86c2','#ffc29e','#efa0cb'][glyph];g.fillRect(19,21,10,8);
        g.fillStyle='#fff2fa';if(glyph===0){g.fillRect(23,21,2,8);g.fillRect(21,23,6,4);}else{
            for(let i=0;i<glyph&&i<3;i++)g.fillRect(20+i*3,23,2,4);
            if(glyph>=4){g.fillRect(21,22,6,1);g.fillRect(21,27,6,1);g.fillRect(20,23,1,4);g.fillRect(27,23,1,4);}
        }return c;
    };
    const glassCanvas=()=>{
        const c=document.createElement('canvas');c.width=c.height=16;const g=c.getContext('2d');
        g.fillStyle='rgba(255,215,237,.16)';g.fillRect(0,0,16,16);g.fillStyle='rgba(255,236,247,.46)';g.fillRect(1,1,14,1);g.fillRect(1,2,1,13);
        g.fillStyle='rgba(240,147,194,.32)';g.fillRect(14,1,1,14);g.fillRect(1,14,13,1);
        g.fillStyle='rgba(255,249,255,.35)';for(let i=0;i<5;i++)g.fillRect(4+i,9-i,1,1);return c;
    };
    async function texture(name,c){
        const t=new Texture({name:name+'.png',namespace:'overload_sim',folder:'block',width:c.width,height:c.height,uv_width:c.width,uv_height:c.height}).fromDataURL(c.toDataURL()).add();await pause();
        t.layers_enabled=true;t.layers.length=0;t.layers.push(new TextureLayer({name:'粉白陶瓷 · 绕组 · 独立升级符号',width:c.width,height:c.height,image_data:c.getContext('2d').getImageData(0,0,c.width,c.height),opacity:100,visible:true,blend_mode:'default'},t));t.selected_layer=t.layers[0];t.updateLayerChanges(true);
        t.path=ROOT+'/src/main/resources/assets/overload_sim/textures/block/'+name+'.png';png('src/main/resources/assets/overload_sim/textures/block/'+name+'.png',c);t.saved=true;return t;
    }
    function project(name){newProject(Formats.java_block);Project.name=name;Project.texture_width=Project.texture_height=32;Project.ambientocclusion=false;}
    function box(group,name,from,to,t,tile=0,cull=false){
        const cube=new Cube({name,from,to,box_uv:false}).init();if(group)cube.addTo(group);
        const u=tile%2*16,v=Math.floor(tile/2)*16;
        for(const [side,f]of Object.entries(cube.faces)){f.texture=t.uuid;f.uv=[u,v,u+16,v+16];f.cullface=cull?side:'';}return cube;
    }
    async function save(name,render='minecraft:cutout'){
        Canvas.updateAll();const model=JSON.parse(Codecs.java_block.compile({raw:false}));
        model.parent='minecraft:block/block';model.render_type=render;model.ambientocclusion=false;
        model.textures.particle='overload_sim:block/'+name;
        write('src/main/resources/assets/overload_sim/models/block/'+name+'.json',model);
        write('art/'+name+'.bbmodel',Codecs.project.compile());Project.save_path=ROOT+'/art/'+name+'.bbmodel';Project.saved=true;
        return model;
    }
    const ids=['simulation_controller','simulation_frame','simulation_efficiency_t1','simulation_efficiency_t2','simulation_efficiency_t3','simulation_fortune_module','simulation_overload_module','simulation_smelting_module'];
    for(let i=0;i<ids.length;i++){
        const id=ids[i];project(id);const t=await texture(id,atlas(i));const base=new Group({name:'粉白外壳'}).init();const detail=new Group({name:i===0?'控制器与水晶徽章':'独立升级模块面板'}).init();
        const c=box(base,'陶瓷与紫灰合金框',[0,0,0],[16,16,16],t,0,true);
        if(i!==1){c.faces.up.uv=[16,16,32,32];c.faces.down.uv=[0,16,16,32];c.faces.north.uv=[16,16,32,32];}
        else{box(detail,'上部紫灰内衬',[2,16,2],[14,16.02,14],t,2);}
        await save(id);
        write('src/main/resources/assets/overload_sim/models/item/'+id+'.json',{parent:'overload_sim:block/'+id});
        if(i===0)write('src/main/resources/assets/overload_sim/blockstates/'+id+'.json',{variants:{'facing=north':{model:'overload_sim:block/'+id},'facing=east':{model:'overload_sim:block/'+id,y:90},'facing=south':{model:'overload_sim:block/'+id,y:180},'facing=west':{model:'overload_sim:block/'+id,y:270}}});
        else write('src/main/resources/assets/overload_sim/blockstates/'+id+'.json',{variants:{'formed=false':{model:'overload_sim:block/'+id},'formed=true':{model:'overload_sim:block/'+(i===1?'simulation_frame_formed':id)}}});
    }
    project('simulation_frame_formed');var ceramic=await texture('simulation_frame_formed',atlas());box(new Group({name:'成型后连续陶瓷边框'}).init(),'平滑边框',[0,0,0],[16,16,16],ceramic,0,true);await save('simulation_frame_formed');
    // Six planes use separate orientations; the proxy retains the original AE glass block state.
    const shapes={north:[[0,0,3],[16,16,4]],south:[[0,0,12],[16,16,13]],west:[[3,0,0],[4,16,16]],east:[[12,0,0],[13,16,16]],up:[[0,12,0],[16,13,16]],down:[[0,3,0],[16,4,16]]};
    const variants={};
    for(const [face,bounds]of Object.entries(shapes)){
        const name='simulation_glass_'+face;project(name);Project.texture_width=Project.texture_height=16;var glass=await texture(name,glassCanvas());const c=box(new Group({name:'内陷粉白透明玻璃板'}).init(),'聚能玻璃内陷板',bounds[0],bounds[1],glass);
        for(const f of Object.values(c.faces))f.uv=[0,0,16,16];
        await save(name,'minecraft:translucent');variants['face='+face]={model:'overload_sim:block/'+name};
    }write('src/main/resources/assets/overload_sim/blockstates/formed_simulation_glass.json',{variants});
    project('simulation_emitter');var winding=await texture('simulation_emitter',atlas());const coils=new Group({name:'悬浮横向聚能线圈'}).init();
    box(coils,'背部固定座',[3,12,3],[13,14,13],winding,2);
    for(let y of [8,10]){
        box(coils,'白色环北',[2,y,2],[14,y+1,4],winding,0);box(coils,'白色环南',[2,y,12],[14,y+1,14],winding,0);box(coils,'白色环西',[2,y,4],[4,y+1,12],winding,0);box(coils,'白色环东',[12,y,4],[14,y+1,12],winding,0);
    }
    box(coils,'粉色绕组北',[3,9,3],[13,10,4],winding,1);box(coils,'粉色绕组南',[3,9,12],[13,10,13],winding,1);box(coils,'粉色绕组西',[3,9,4],[4,10,12],winding,1);box(coils,'粉色绕组东',[12,9,4],[13,10,12],winding,1);
    await save('simulation_emitter');
    // A scaled assembled 3×3×3 project is editable and stays within Java model coordinate limits.
    project('multiblock_simulation_assembly');var skin=await texture('multiblock_simulation_assembly',atlas());var pane=await texture('multiblock_simulation_assembly_glass',glassCanvas());
    const frame=new Group({name:'连续框架'}).init(),panes=new Group({name:'五面内陷玻璃'}).init(),emitters=new Group({name:'内部顶部四线圈'}).init(),focus=new Group({name:'悬浮水晶展示位置'}).init();
    for(let x of [0,13])for(let z of [0,13])box(frame,'竖直连续边框',[x,0,z],[x+3,16,z+3],skin,0);
    for(let y of [0,13]){box(frame,'横向前框',[3,y,0],[13,y+3,3],skin);box(frame,'横向后框',[3,y,13],[13,y+3,16],skin);box(frame,'侧向左框',[0,y,3],[3,y+3,13],skin);box(frame,'侧向右框',[13,y,3],[16,y+3,13],skin);}
    for(let y of [3])box(frame,'底板',[3,y-1,3],[13,y,13],skin,3);
    for(const [name,from,to]of [['前玻璃',[3,3,3.7],[13,13,3.9]],['后玻璃',[3,3,12.1],[13,13,12.3]],['左玻璃',[3.7,3,3],[3.9,13,13]],['右玻璃',[12.1,3,3],[12.3,13,13]],['顶玻璃',[3,12.1,3],[13,12.3,13]]]){
        var c=box(panes,name,from,to,pane);for(var f of Object.values(c.faces))f.uv=[0,0,16,16];
    }
    for(let x of [4,10])for(let z of [4,10]){box(emitters,'线圈北',[x,11,z],[x+2,11.6,z+.45],skin,1);box(emitters,'线圈南',[x,11,z+1.55],[x+2,11.6,z+2],skin,1);box(emitters,'线圈左',[x,11,z+.45],[x+.45,11.6,z+1.55],skin,0);box(emitters,'线圈右',[x+1.55,11,z+.45],[x+2,11.6,z+1.55],skin,0);}
    for(let x of [5.2,7.2,9.2])for(let z of [5.2,7.2,9.2])box(focus,'粉色晶簇位置',[x,6,z],[x+1.3,8.8,z+1.3],skin,3);
    Canvas.updateAll();write('art/multiblock_simulation_assembly.bbmodel',Codecs.project.compile());Project.save_path=ROOT+'/art/multiblock_simulation_assembly.bbmodel';Project.saved=true;
    Preview.selected.loadAnglePreset({position:[30,26,33],target:[8,8,8],projection:'perspective'});await pause();
    Screencam.screenshotPreview(Preview.selected,{width:800,height:800},url=>Blockbench.writeFile(ROOT+'/art/multiblock_simulation_preview.png',{savetype:'image',content:url}));
    write('art/multiblock-export.json',{source:'Blockbench desktop canvas and Java model codec',playerBlocks:8,formedFrame:true,glassPlanes:6,emitter:true,assemblyLayers:4,preview:'multiblock_simulation_preview.png'});
    Blockbench.showQuickMessage('多方块模拟室的粉白模型、玻璃、线圈和分层工程已导出',7000);
};
