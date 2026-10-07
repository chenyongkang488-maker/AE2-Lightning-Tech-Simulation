// Original pixels drawn inside native Blockbench; qiqi and contributors, MIT.
// Preserve the chamber's pink palette; every temporal term has a full-circle period.
globalThis.drawContinuousLaserFlow=async function(ROOT){
    const flow=document.createElement('canvas');flow.width=32;flow.height=32*64;
    const g=flow.getContext('2d');g.imageSmoothingEnabled=false;
    const palette=['#572444','#b74883','#f69ac9','#fff0fb'];
    for(let frame=0;frame<64;frame++){
        const phase=frame*Math.PI*2/64;
        for(let y=0;y<32;y++)for(let x=0;x<32;x++){
            const band=Math.sin(x*.43+Math.sin(y*.32+phase)*2.2-phase)+Math.sin(y*.35-x*.16+phase*2);
            const sparkle=(x*17+y*31)%53===0;
            g.fillStyle=palette[sparkle?3:band>.9?2:band>-.8?1:0];g.fillRect(x,frame*32+y,1,1);
        }
    }
    newProject(Formats.java_block);Project.name='Continuous pink laser flow';Project.texture_width=Project.texture_height=32;Project.ambientocclusion=false;
    const t=new Texture({name:'laser_energy_flow.png',namespace:'overload_sim',folder:'block',width:32,height:2048,uv_width:32,uv_height:32}).fromDataURL(flow.toDataURL()).add();
    await new Promise(resolve=>setTimeout(resolve,150));
    t.layers_enabled=true;t.layers.length=0;
    t.layers.push(new TextureLayer({name:'64帧首尾连续粉色流纹',width:32,height:2048,image_data:g.getImageData(0,0,32,2048),opacity:100,visible:true,blend_mode:'default'},t));
    t.selected_layer=t.layers[0];t.updateLayerChanges(true);t.frame_time=2;
    t.path=ROOT+'/src/main/resources/assets/overload_sim/textures/block/laser_energy_flow.png';
    Blockbench.writeFile(t.path,{savetype:'image',content:flow.toDataURL()});t.saved=true;
    const cube=new Cube({name:'光束材质预览 · 最近邻像素',from:[0,0,0],to:[16,16,16],box_uv:false}).init();
    for(const face of Object.values(cube.faces)){face.texture=t.uuid;face.uv=[0,0,32,32];}
    const write=(path,value)=>Blockbench.writeFile(ROOT+'/'+path,{savetype:'text',content:JSON.stringify(value,null,2)+'\n'});
    write('src/main/resources/assets/overload_sim/textures/block/laser_energy_flow.png.mcmeta',{animation:{width:32,height:32,frametime:2,interpolate:true}});
    write('src/main/resources/assets/overload_sim/models/block/laser_energy_flow.json',{parent:'minecraft:block/cube_all',textures:{all:'overload_sim:block/laser_energy_flow','0':'overload_sim:block/laser_energy_flow'}});
    Canvas.updateAll();Project.save_path=ROOT+'/art/blockbench/laser-energy-flow.bbmodel';
    Blockbench.writeFile(Project.save_path,{savetype:'text',content:Codecs.project.compile()});Project.saved=true;
    Blockbench.showQuickMessage('64帧粉色激光首尾连续流纹已导出',3500);
};
