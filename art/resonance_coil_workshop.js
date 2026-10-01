// Coil artwork created/exported in Blockbench; module cases adapt AE2LT (see THIRD_PARTY_NOTICES.md).
(function(){
    const ROOT='D:/MinecraftDev/OverloadSimulation';let drawAction,previewAction;
    const canvas=(w,h)=>{let c=document.createElement('canvas');c.width=w;c.height=h;return c;};
    const write=(p,content)=>Blockbench.writeFile(ROOT+'/'+p,{savetype:'text',content:typeof content==='string'?content:JSON.stringify(content,null,2)+'\n'});
    const png=(p,c)=>Blockbench.writeFile(ROOT+'/'+p,{savetype:'image',content:c.toDataURL()});
    const pause=()=>new Promise(r=>setTimeout(r,120));
    function metal(){
        const c=canvas(64,64),g=c.getContext('2d');
        const palettes=[['#c1c3d3','#f4f3fa','#ffffff','#999aad'],['#39374d','#58576c','#8d8b9f','#262739'],['#bd6599','#f0a7d0','#ffe3f5','#824b74'],['#685578','#c4a8da','#e3d6f1','#434058'],['#477d92','#b6eaff','#e8ffff','#36445e'],['#626176','#aaa8ba','#d4d0de','#3d3c51'],['#954778','#de78b7','#ffbfdf','#633356'],['#c5a5b7','#ffe9f4','#ffffff','#897287']];
        for(let t=0;t<16;t++){
            const x=t%4*16,y=Math.floor(t/4)*16,p=palettes[t%8];g.fillStyle=p[0];g.fillRect(x,y,16,16);g.fillStyle=p[1];g.fillRect(x+1,y+1,14,14);g.fillStyle=p[2];g.fillRect(x+1,y+1,14,1);g.fillRect(x+1,y+1,1,14);g.fillStyle=p[3];g.fillRect(x+2,y+14,13,1);g.fillRect(x+14,y+2,1,12);
            if(t%8===1){for(let n=4;n<14;n+=3){g.fillStyle='#b686a7';g.fillRect(x+2,y+n,11,1);}g.fillStyle='#242536';g.fillRect(x+5,y+2,5,12);}
            if(t%8===2){for(let n=3;n<14;n+=4){g.fillStyle='#c879a8';g.fillRect(x+n,y+2,1,12);g.fillStyle='#fff0fc';g.fillRect(x+n+1,y+2,1,12);}}
            if(t%8===4){g.fillStyle='#577b98';g.fillRect(x+3,y+3,10,10);g.fillStyle='#a9ebff';g.fillRect(x+4,y+4,8,8);g.fillStyle='#f7ffff';g.fillRect(x+6,y+5,2,5);}
            if(t===11){g.fillStyle='#666074';g.fillRect(x+2,y+2,12,12);for(let py=4;py<12;py++)for(let px=5;px<11;px++)if(Math.abs(px-7.5)+Math.abs(py-7.5)<4){g.fillStyle=px<8?'#ffe5f6':'#e18fbd';g.fillRect(x+px,y+py,1,1);}g.fillStyle='#bbeeff';g.fillRect(x+5,y+9,1,2);}
        }return c;
    }
    function stroke(g,pts,color,offset){
        g.strokeStyle=color;g.lineWidth=1;g.beginPath();pts.forEach(([x,y],i)=>i?g.lineTo(x+.5,y+.5+offset):g.moveTo(x+.5,y+.5+offset));g.stroke();
    }
    function lightning(){
        const c=canvas(32,32*8),g=c.getContext('2d');g.imageSmoothingEnabled=false;
        for(let f=0;f<8;f++){
            const center=[16+(f%3-1)*.3,16],starts=[[3,3],[28,3],[3,28],[28,28]];
            starts.forEach(([sx,sy],arm)=>{
                const pts=[];for(let n=0;n<=5;n++){
                    const t=n/5,j=n===0||n===5?0:Math.sin(f*2.4+arm*1.9+n*3.1)*2.1;
                    pts.push([sx+(center[0]-sx)*t+j,sy+(center[1]-sy)*t-j]);
                }
                g.save();g.translate(0,f*32);g.lineJoin='bevel';g.lineCap='square';
                for(const [color,width]of [['#ef6bbd',2.5],['#ffd0ee',1.4],['#fff0ff',.75]]){
                    g.strokeStyle=color;g.lineWidth=width;g.beginPath();pts.forEach(([x,y],i)=>i?g.lineTo(x,y):g.moveTo(x,y));g.stroke();
                }
                g.restore();
            });
            g.fillStyle=f%2?'#fff7ff':'#ffd0ee';g.fillRect(15,15+f*32,2,2);
        }return c;
    }
    // AE2LT overload_module_base.png; CC BY-NC-SA 3.0, see THIRD_PARTY_NOTICES.md.
    const MODULE_BASE='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAAAeUExURaM6e05TWt7f4sHDxzU5QKWnrGFmbe2Rvd9loAAAAF19jGoAAAAKdFJOU////////////wCyzCzPAAAACXBIWXMAAA7BAAAOwQG4kWvtAAAAYklEQVQoU12PCwrAMAhD/WRtc/8Lj7RD6iSgPIwfI0mzT6pJv0LAIyIjUymdRsRTsYFnZgNA9QDoIEMdw70sf3As19BjaWv3UB2ks5Q2GD5KB8Am5sI0uAABW1gmYX/b338BWdwElmYD00UAAAAASUVORK5CYII=';
    async function modules(){
        const base=new Image();base.src=MODULE_BASE;await base.decode();
        const ids=['extreme_voltage_module','mimic_tool_module','ultimate_destruction_module','efficiency_module','fortune_module','silk_touch_module','wrench_module'];
        const colors=['#eb78b7','#acbde7','#b35b9d','#eca4cb','#e8cb7e','#a4dfef','#eb90c4'];
        const glyphs=[[[9,4],[8,5],[7,6],[8,6],[9,6],[8,7],[7,8]],[[6,4],[7,4],[8,4],[9,4],[7,5],[9,5],[7,6],[7,7],[7,8]],[[6,4],[7,4],[8,4],[9,4],[6,5],[9,5],[6,6],[8,6],[9,6],[6,7],[7,7],[9,7],[6,8],[7,8],[8,8],[9,8]],[[6,4],[7,5],[8,6],[7,7],[6,8],[8,4],[9,5],[10,6],[9,7],[8,8]],[[7,4],[8,4],[6,5],[9,5],[6,6],[9,6],[7,7],[8,7],[7,8]],[[7,4],[8,4],[6,5],[9,5],[6,6],[9,6],[7,7],[8,7]],[[6,4],[9,4],[6,5],[7,5],[8,5],[9,5],[7,6],[8,6],[8,7],[9,8]]];
        const preview=canvas(7*64,64),pg=preview.getContext('2d');pg.imageSmoothingEnabled=false;
        for(let index=0;index<ids.length;index++){
            const c=canvas(16,16),g=c.getContext('2d');g.imageSmoothingEnabled=false;g.drawImage(base,0,0);
            g.fillStyle='#54586f';g.fillRect(5,3,6,6);
            g.fillStyle=colors[index];for(const [x,y]of glyphs[index])g.fillRect(x,y,1,1);
            g.fillStyle='#fff1fa';const [hx,hy]=glyphs[index][0];g.fillRect(hx,hy,1,1);
            png('src/main/resources/assets/overload_sim/textures/item/'+ids[index]+'.png',c);pg.drawImage(c,index*64,0,64,64);
        }
        png('art/coil_modules_preview.png',preview);
    }
    async function texture(name,c,animated=false){
        const t=new Texture({name:name+'.png',namespace:'overload_sim',folder:'item',width:c.width,height:c.height,uv_width:c.width,uv_height:animated?32:c.height}).fromDataURL(c.toDataURL()).add();await pause();
        t.layers_enabled=true;t.layers.length=0;
        t.layers.push(new TextureLayer({name:animated?'粉色电流 · 8 帧':'粉白金属 · 紫灰握柄 · 绕组',width:c.width,height:c.height,image_data:c.getContext('2d').getImageData(0,0,c.width,c.height),opacity:100,visible:true,blend_mode:'default'},t));t.selected_layer=t.layers[0];t.updateLayerChanges(true);
        png('src/main/resources/assets/overload_sim/textures/item/'+name+'.png',c);t.path=ROOT+'/src/main/resources/assets/overload_sim/textures/item/'+name+'.png';t.saved=true;if(animated)t.frame_time=2;return t;
    }
    async function draw(){
        newProject(Formats.java_block);Project.name='resonance_coil';Project.java_block_version='1.9.0';Project.texture_width=Project.texture_height=64;Project.ambientocclusion=false;
        const body=await texture('resonance_coil_body',metal());const fx=await texture('resonance_coil_lightning',lightning(),true);body.use_as_default=true;
        function box(name,from,to,tile=0,rotation=0){
            const cube=new Cube({name,from,to,box_uv:false,rotation:[0,0,rotation],origin:from.map((v,i)=>(v+to[i])/2)}).init();
            const u=tile%4*16,v=Math.floor(tile/4)*16;
            for(const [side,f]of Object.entries(cube.faces)){f.texture=body.uuid;f.uv=[u,v,u+16,v+16];f.cullface='';}return cube;
        }
        function ring(name,r,width,length,z,depth,tile){
            for(let n=0;n<8;n++){
                const theta=n*Math.PI/4,cx=8+Math.sin(theta)*r,cy=13.5+Math.cos(theta)*r;
                let angle=-n*45;while(angle<=-90)angle+=180;while(angle>90)angle-=180;
                let dx=length,dy=width;if(Math.abs(angle)===90){[dx,dy]=[dy,dx];angle=0;}
                box(name+' '+(n+1),[cx-dx/2,cy-dy/2,z],[cx+dx/2,cy+dy/2,z+depth],tile,angle);
            }
        }
        box('科技短柄',[7,1,7],[9,8.3,9],1);
        box('白色握柄护甲左',[6.8,1.3,6.8],[7.25,7,9.2],0);box('白色握柄护甲右',[8.75,1.3,6.8],[9.2,7,9.2],0);
        for(let y of [1.7,4.6,6.7])box('粉色绝缘环',[6.7,y,6.7],[9.3,y+.45,9.3],2);
        box('尾部限位帽',[6.5,.5,6.5],[9.5,1.5,9.5],5);box('尾部灯',[7.3,.45,6.4],[8.7,1.4,6.55],4);
        box('护手',[5.3,7.4,6.3],[10.7,8.4,9.7],0);box('粉色护手线',[5.4,7.35,6.25],[10.6,7.6,6.45],2);
        box('线圈柄连接器',[6.7,8.1,6.5],[9.3,10,9.5],5);box('侧边能量灯',[9.4,5.6,7],[10.3,7.1,9],4);
        const floatingStart=Cube.all.length;
        ring('后侧紫灰屏蔽圈',4.7,1.25,3.4,8.8,1.3,3);
        ring('粉色绕组',4.55,1.1,3.5,7.4,1.45,2);
        ring('前侧粉白陶瓷圈',4.7,.85,3.35,6.2,1.1,0);
        for(let n=0;n<4;n++){
            const x=n%2?12.6:2.2,y=n>=2?16:10;
            box('绕组固定夹',[x,y,5.9],[x+1.2,y+1.2,9.9],5);
            box('固定夹粉色灯',[x+.2,y+.2,5.82],[x+1,y+1,5.98],6);
        }
        box('完美水晶徽章',[6.8,8.5,5.95],[9.2,10.1,6.25],11);
        function lightningPlane(name,z){const c=new Cube({name,from:[4.4,9.9,z],to:[11.6,17.1,z],box_uv:false}).init();for(const [side,f]of Object.entries(c.faces)){f.texture=['north','south'].includes(side)?fx.uuid:null;f.uv=[0,0,64,64];f.cullface='';}}
        lightningPlane('独立粉色闪电 · 正面',6.1);lightningPlane('独立粉色闪电 · 背面',10.15);
        // Rotate the complete head into the XZ plane and levitate it above the grip.
        // Keep the winding, clamps, badge and both animated layers in the same floating assembly.
        const transform=p=>[p[0],p[2]+6.6,p[1]-5.5];
        for(const c of Cube.all.slice(floatingStart)){
            c.from=transform(c.from);c.to=transform(c.to);c.origin=transform(c.origin);c.rotation=[0,-c.rotation[2],0];
            const faces={};for(const [side,face]of Object.entries(c.faces))faces[side]=face.getSaveCopy();
            const directions={north:'down',south:'up',up:'south',down:'north',east:'east',west:'west'};
            for(const [before,after]of Object.entries(directions))c.faces[after].extend(faces[before]);
        }
        Canvas.updateAll();
        const display={gui:{rotation:[35,215,-15],translation:[0,-1,0],scale:[.8,.8,.8]},ground:{rotation:[0,0,0],translation:[0,2,0],scale:[.25,.25,.25]},fixed:{rotation:[0,0,0],translation:[0,-1,0],scale:[.65,.65,.65]},thirdperson_righthand:{rotation:[0,-90,0],translation:[0,1,1],scale:[.65,.65,.65]},thirdperson_lefthand:{rotation:[0,90,0],translation:[0,1,1],scale:[.65,.65,.65]},firstperson_righthand:{rotation:[25,-90,8],translation:[0,1.5,1],scale:[.58,.58,.58]},firstperson_lefthand:{rotation:[25,90,-8],translation:[0,1.5,1],scale:[.58,.58,.58]}};
        for(const [key,value]of Object.entries(display))Project.display_settings[key]=new DisplaySlot(key,value);
        const model=JSON.parse(Codecs.java_block.compile({raw:false}));model.parent='minecraft:block/block';model.render_type='minecraft:cutout';model.ambientocclusion=false;model.display=display;model.textures.particle='overload_sim:item/resonance_coil_body';
        write('src/main/resources/assets/overload_sim/models/item/resonance_coil.json',model);
        write('src/main/resources/assets/overload_sim/textures/item/resonance_coil_lightning.png.mcmeta',{animation:{width:32,height:32,frametime:2,interpolate:false}});
        Project.save_path=ROOT+'/art/resonance_coil.bbmodel';write('art/resonance_coil.bbmodel',Codecs.project.compile());Project.saved=true;
        await modules();
        Preview.selected.loadAnglePreset({position:[24,32,30],target:[8,9,8],projection:'perspective'});await pause();
        Screencam.screenshotPreview(Preview.selected,{width:640,height:640},url=>Blockbench.writeFile(ROOT+'/art/resonance_coil_preview.png',{savetype:'image',content:url}));
        write('art/coil-export.json',{source:'Blockbench canvas and Java item codec',elements:model.elements.length,lightningFrames:8,modules:7,horizontalFloatingHead:true,convergingArcs:true,moduleBase:"AE2LT overload_module_base.png (CC BY-NC-SA 3.0)"});
        Blockbench.showQuickMessage('谐振雷鸣线圈与 7 个模块已绘制、保存和导出',6000);
    }
    Plugin.register('resonance_coil_workshop',{title:'Resonance Coil Workshop',author:'Codex',description:'Original horizontal floating coil and animated lightning artwork.',icon:'bolt',version:'0.3.0',variant:'desktop',
        onload(){drawAction=new Action('draw_resonance_coil',{name:'绘制谐振雷鸣线圈',icon:'bolt',click(){draw().catch(e=>{write('art/coil-error.txt',String(e.stack||e));Blockbench.showMessageBox({title:'线圈导出错误',message:String(e.stack||e)});});}});previewAction=new Action('preview_resonance_coil',{name:'导出线圈预览',icon:'photo_camera',click(){Screencam.screenshotPreview(Preview.selected,{width:640,height:640},url=>Blockbench.writeFile(ROOT+'/art/resonance_coil_preview.png',{savetype:'image',content:url}));}});MenuBar.addAction(drawAction,'tools');MenuBar.addAction(previewAction,'tools');},
        onunload(){drawAction?.delete();previewAction?.delete();}
    });
})();
