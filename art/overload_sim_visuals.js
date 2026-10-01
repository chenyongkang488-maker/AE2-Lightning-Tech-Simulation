// Local Blockbench workshop. All bitmap drawing, layers, model creation and exports run in Blockbench.
// The embedded electro-chime silhouette is from AE2 Lightning Tech Reborn 2.1.0 (CC BY-NC-SA 3.0).
// See THIRD_PARTY_NOTICES.md; the frame geometry and lightning strokes are original addon art.
(function(){
    const ROOT='D:/MinecraftDev/OverloadSimulation';
    const REFERENCE='data:image/png;base64,iVBORw0KGgoAAAANSUhEUgAAABAAAAAQCAMAAAAoLQ9TAAAAAXNSR0IArs4c6QAAAARnQU1BAACxjwv8YQUAAABCUExURWYUSv/l/fS008xRjj8MH5k9YOVytSYrVDxfgP+A1/7//2BUprBv3dr//5FdzTc7coq775zT/2yQsylAVxoXLwAAAOP7YkEAAAAWdFJOU////////////////////////////wAB0sDkAAAACXBIWXMAAA7CAAAOwgEVKEqAAAAAgUlEQVQoU13O6w7DIAgFYLwerVY6hfd/1UXbrev4QeALCYf0r+g7XNMHyDyBjH0AGeMuObu3NkRSAEqaUlJvXYgeOS/YtuRjgfO1XrDvVPKExguO115yRaC5T+j9QCsIXBhjfhm9o7EwGH2BjiSFBZgHZ44hDBHIuKOLiIrcSX/qDfTdD0MQhy32AAAAAElFTkSuQmCC';
    let generateAction,saveAction,previewAction,multiblockAction;
    const canvas=(w,h)=>{const c=document.createElement('canvas');c.width=w;c.height=h;return c;};
    const writeText=(path,value)=>Blockbench.writeFile(ROOT+'/'+path,{savetype:'text',content:typeof value==='string'?value:JSON.stringify(value,null,2)+'\n'});
    const writePng=(path,c)=>Blockbench.writeFile(ROOT+'/'+path,{savetype:'image',content:c.toDataURL('image/png')});
    const image=source=>new Promise((resolve,reject)=>{const img=new Image();img.onload=()=>resolve(img);img.onerror=()=>reject(new Error('Cannot load reference sprite'));img.src=source;});
    const pause=()=>new Promise(resolve=>setTimeout(resolve,100));
    function body(source,stage){
        const c=canvas(16,16),ctx=c.getContext('2d');ctx.drawImage(source,0,0);const pixels=ctx.getImageData(0,0,16,16);
        const pink=['#643852','#ac5989','#e987b2','#ffc3df','#fff4fc'];
        for(let y=0;y<16;y++)for(let x=0;x<16;x++){
            const i=(y*16+x)*4;if(!pixels.data[i+3])continue;
            const [r,g,b]=pixels.data.slice(i,i+3);const luminance=.2126*r+.7152*g+.0722*b;
            if(stage<2){const gray=Math.min(250,Math.round(62+luminance*.76+(stage===1?10:0)));pixels.data[i]=pixels.data[i+1]=pixels.data[i+2]=gray;}
            else{
                let color=pink[Math.min(4,Math.floor(luminance/52))];
                // Mostly pink; small cool facets and warm tips retain the clustered silhouette.
                if(x<=4&&b>r+20)color=luminance>170?'#d3ffff':'#8daacb';
                if(x>=11&&luminance>120)color=luminance>200?'#ece2ff':'#b4a3e0';
                if(y>=11&&x<=5&&luminance>160)color='#f5d9ad';
                const n=parseInt(color.slice(1),16);pixels.data[i]=n>>16;pixels.data[i+1]=(n>>8)&255;pixels.data[i+2]=n&255;
            }
        }
        ctx.putImageData(pixels,0,0);return c;
    }
    function line(ctx,points,color,offset=0){
        ctx.fillStyle=color;
        for(let n=1;n<points.length;n++){
            let [x,y]=points[n-1], [tx,ty]=points[n];let dx=Math.abs(tx-x),sx=x<tx?1:-1,dy=-Math.abs(ty-y),sy=y<ty?1:-1,err=dx+dy;
            for(;;){ctx.fillRect(x,y+offset,1,1);if(x===tx&&y===ty)break;const e=2*err;if(e>=dy){err+=dy;x+=sx;}if(e<=dx){err+=dx;y+=sy;}}
        }
    }
    function lightning(){
        const c=canvas(16,16*12),ctx=c.getContext('2d');
        const paths=[[[3,2],[1,4],[3,5],[1,8],[2,10]],[[12,3],[14,5],[12,6],[14,9],[13,11]],[[4,14],[6,13],[7,15],[10,13],[12,14]]];
        for(let f=0;f<12;f++){
            const o=f*16;const count=f>=9?1:2;
            for(let j=0;j<count;j++){
                const path=paths[(f+j)%3].map(([x,y],i)=>[Math.clamp(x+((f+i)%4===0?1:0),0,15),y]);
                line(ctx,path,f>=9?'#efafd5':'#ee76c3',o);
                const bright=path.filter((_,i)=>i%2===f%2);for(const [x,y]of bright){ctx.fillStyle='#fff8ff';ctx.fillRect(x,y+o,1,1);}
            }
            if(f<8){ctx.fillStyle=f%2?'#bdefff':'#ffe5fb';ctx.fillRect(7+f%3,1+o,1,1);}
        }
        return c;
    }
    function surface(){
        const c=canvas(64,64),ctx=c.getContext('2d');
        const fill=(x,y,w,h,color)=>{ctx.fillStyle=color;ctx.fillRect(x,y,w,h);};
        for(let tile=0;tile<16;tile++){
            const x=tile%4*16,y=Math.floor(tile/4)*16;
            fill(x,y,16,16,'#b6b8c7');fill(x+1,y+1,14,14,'#e4e4eb');fill(x+2,y+2,12,12,'#faf4fa');
            if(tile===0){fill(x,y+12,16,2,'#d793b7');fill(x,y+14,16,2,'#77778c');}
            if(tile===1){fill(x+2,y,2,16,'#9a9caf');fill(x+5,y,2,16,'#ffffff');fill(x,y+10,16,2,'#f3a2cf');}
            if(tile===2){fill(x,y,16,16,'#66697e');fill(x,y+2,16,1,'#dddce8');fill(x,y+5,16,2,'#e595c3');fill(x,y+10,16,2,'#34374d');}
            if(tile===3){
                fill(x+2,y+2,12,12,'#f1abd0');fill(x+3,y+3,10,10,'#fff4fc');fill(x+5,y+5,6,6,'#79798e');
                for(let py=5;py<=10;py++)for(let px=5;px<=10;px++)if(Math.abs(px-7.5)+Math.abs(py-7.5)<3)fill(x+px,y+py,1,1,px<8?'#fff0fa':'#e782bc');
            }
            if(tile===4){fill(x,y,16,16,'#78758f');fill(x+1,y+1,14,14,'#e89bc8');fill(x+3,y+3,10,10,'#fbdaec');fill(x+5,y+5,6,6,'#fff7fc');}
            if(tile===5){fill(x,y,16,16,'#5c5d72');fill(x+2,y+2,12,12,'#aaaabd');fill(x+3,y+3,10,10,'#d1ccd9');}
            if(tile===6){fill(x,y,16,16,'#f2b4d6');fill(x+1,y+1,14,14,'#fff6fc');fill(x+2,y+2,12,12,'#d97dad');}
        }
        return c;
    }
    function glass(){
        const c=canvas(16,16),ctx=c.getContext('2d');
        // Binary alpha keeps the crystal clear through Minecraft's cutout render layer.
        ctx.fillStyle='#d9bacd';ctx.fillRect(0,0,16,1);ctx.fillRect(0,15,16,1);ctx.fillRect(0,1,1,14);ctx.fillRect(15,1,1,14);
        ctx.fillStyle='#f4edf7';for(const [x,y]of [[2,3],[3,2],[3,5],[4,4],[5,3],[10,12],[11,11],[12,10],[12,13],[13,12]])ctx.fillRect(x,y,1,1);
        ctx.fillStyle='#b9dce6';ctx.fillRect(2,4,1,1);ctx.fillRect(11,13,1,1);
        return c;
    }
    async function texture(name,folder,c,layers=[]){
        const tex=new Texture({name:name+'.png',namespace:'overload_sim',folder,width:c.width,height:c.height,uv_width:c.width,uv_height:name.endsWith('_lightning')?16:c.height}).fromDataURL(c.toDataURL()).add();
        await pause();
        if(layers.length){
            tex.layers_enabled=true;tex.layers.length=0;
            for(const [label,pixels]of layers){const ctx=pixels.getContext('2d');const layer=new TextureLayer({name:label,width:pixels.width,height:pixels.height,image_data:ctx.getImageData(0,0,pixels.width,pixels.height),opacity:100,visible:true,blend_mode:'default'},tex);tex.layers.push(layer);}
            tex.selected_layer=tex.layers[tex.layers.length-1];tex.updateLayerChanges(true);
        }
        const path='src/main/resources/assets/overload_sim/textures/'+folder+'/'+name+'.png';writePng(path,c);tex.path=ROOT+'/'+path;tex.saved=true;return tex;
    }
    function plane(name,tex,from,to,exported=false){
        const cube=new Cube({name,from,to,box_uv:false,export:exported}).init();
        const uvSize=Format.per_texture_uv_size?tex.uv_width:Project.texture_width;
        for(const [key,face]of Object.entries(cube.faces)){face.texture=(key==='north'||key==='south')?tex.uuid:null;face.uv=[0,0,uvSize,uvSize];}return cube;
    }
    function saveProject(name){Project.save_path=ROOT+'/art/'+name+'.bbmodel';writeText('art/'+name+'.bbmodel',Codecs.project.compile());Project.saved=true;}
    async function generate(){
        const reference=await image(REFERENCE);const blank=body(reference,0),bound=body(reference,1),perfect=body(reference,2),arcs=lightning(),frame=surface();
        newProject(Formats.free);Project.name='simulation_crystals_layers';Project.texture_width=Project.texture_height=16;
        const grayTex=await texture('blank_simulation_crystal','item',blank,[['灰白水晶本体',blank]]);
        const boundTex=await texture('simulation_crystal','item',bound,[['灰白绑定水晶',bound]]);
        const perfectTex=await texture('perfect_simulation_crystal','item',perfect,[['粉彩水晶本体',perfect]]);
        const fxTex=await texture('perfect_simulation_crystal_lightning','item',arcs,[['动态闪电：12 帧',arcs]]);
        fxTex.frame_time=2;
        writeText('src/main/resources/assets/overload_sim/textures/item/perfect_simulation_crystal_lightning.png.mcmeta',{animation:{width:16,height:16,frametime:2,interpolate:false,frames:[0,1,2,3,4,5,6,7,8,{index:9,time:4},{index:10,time:4},{index:11,time:4}]}});
        plane('空白灰白水晶',grayTex,[-18,2,8],[-6,14,8]);plane('绑定灰白水晶',boundTex,[2,2,8],[14,14,8]);plane('完美粉彩水晶',perfectTex,[22,2,8],[34,14,8]);plane('独立闪电层',fxTex,[22,2,7.99],[34,14,7.99]);
        perfectTex.select();Canvas.updateAll();saveProject('simulation_crystals_layers');
        const first=canvas(16,16);first.getContext('2d').drawImage(perfect,0,0);first.getContext('2d').drawImage(arcs,0,0,16,16,0,0,16,16);
        const logo=canvas(128,128),logoCtx=logo.getContext('2d');logoCtx.imageSmoothingEnabled=false;logoCtx.drawImage(first,8,8,112,112);writePng('src/main/resources/logo.png',logo);
        const sheet=canvas(640,192),sheetCtx=sheet.getContext('2d');sheetCtx.imageSmoothingEnabled=false;sheetCtx.fillStyle='#242532';sheetCtx.fillRect(0,0,640,192);
        for(const [i,sprite]of [blank,bound,first].entries())sheetCtx.drawImage(sprite,32+i*208,20,144,144);writePng('art/crystal_preview.png',sheet);
        newProject(Formats.java_block);Project.java_block_version='1.9.0';Project.name='overload_simulation_chamber_hollow';Project.texture_width=Project.texture_height=64;Project.ambientocclusion=false;
        const metal=await texture('overload_simulation_chamber','block',frame,[['粉白金属与磁场纹样',frame]]);metal.use_as_default=true;
        function cube(name,from,to,tile=0){
            const c=new Cube({name,from,to,box_uv:false}).init(),dx=to[0]-from[0],dy=to[1]-from[1],dz=to[2]-from[2],u=tile%4*16,v=Math.floor(tile/4)*16;
            for(const [side,f]of Object.entries(c.faces)){const [w,h]=side==='up'||side==='down'?[dx,dz]:side==='east'||side==='west'?[dz,dy]:[dx,dy];f.texture=metal.uuid;f.uv=[u,v,u+w,v+h];f.cullface='';}return c;
        }
        cube('下部防护底座',[0,0,0],[16,2,16],2);cube('白色悬浮平台',[1,2,1],[15,3,15],0);
        for(const [x,z]of [[0,0],[0,14.5],[14.5,0],[14.5,14.5]]){
            cube('角部立柱',[x,2,z],[x+1.5,14,z+1.5],1);cube('粉色绝缘环',[x-.05,10,z-.05],[x+1.55,11,z+1.55],6);
        }
        cube('左侧上框',[0,14,0],[2,16,16],0);cube('右侧上框',[14,14,0],[16,16,16],0);cube('前侧上框',[2,14,0],[14,16,2],0);cube('后侧上框',[2,14,14],[14,16,16],0);
        for(const [x,z]of [[2,2],[2,11.5],[11.5,2],[11.5,11.5]]){
            cube('四向磁场发生器',[x,4,z],[x+2.5,6,z+2.5],4);cube('磁场支架',[x+.75,3,z+.75],[x+1.75,4,z+1.75],5);
        }
        cube('悬浮场下环',[5.5,3,5.5],[10.5,3.5,10.5],4);
        const badge=cube('完美水晶标志',[6,12.5,-.15],[10,16,.1],3);badge.faces.north.uv=badge.faces.south.uv=[48,0,64,16];
        const glassCanvas=glass();
        const glassTex=await texture('simulation_chamber_glass','block',glassCanvas,[['玻璃边框与反光',glassCanvas]]);
        function glassPanel(name,from,to,sides){
            const pane=new Cube({name,from,to,box_uv:false}).init();
            for(const [side,face]of Object.entries(pane.faces)){face.texture=sides.includes(side)?glassTex.uuid:null;face.uv=[0,0,64,64];face.cullface='';}
        }
        glassPanel('北面透明玻璃',[1.5,3,.25],[14.5,14,.5],['north','south']);
        glassPanel('南面透明玻璃',[1.5,3,15.5],[14.5,14,15.75],['north','south']);
        glassPanel('西面透明玻璃',[.25,3,1.5],[.5,14,14.5],['west','east']);
        glassPanel('东面透明玻璃',[15.5,3,1.5],[15.75,14,14.5],['west','east']);
        glassPanel('顶部透明玻璃',[2,15.5,2],[14,15.75,14],['up','down']);
        glassPanel('底部玻璃保护层',[2,3.05,2],[14,3.3,14],['up','down']);
        const model=JSON.parse(Codecs.java_block.compile({raw:false}));model.parent='minecraft:block/block';model.render_type='minecraft:cutout';model.ambientocclusion=false;model.textures.particle='overload_sim:block/overload_simulation_chamber';
        model.display={gui:{rotation:[30,225,0],translation:[0,0,0],scale:[.65,.65,.65]},ground:{rotation:[0,0,0],translation:[0,3,0],scale:[.25,.25,.25]},fixed:{rotation:[0,0,0],translation:[0,0,0],scale:[.5,.5,.5]},thirdperson_righthand:{rotation:[75,45,0],translation:[0,2.5,0],scale:[.375,.375,.375]}};
        writeText('src/main/resources/assets/overload_sim/models/block/overload_simulation_chamber.json',model);
        // Preview-only crossed sprite planes are never exported into the empty machine's model.
        const previewBody=await texture('perfect_simulation_crystal','item',perfect,[['悬浮水晶预览',perfect]]);
        const previewFx=await texture('perfect_simulation_crystal_lightning','item',arcs,[['闪电预览（游戏中动态渲染）',arcs]]);previewFx.frame_time=2;
        plane('仅预览：悬浮水晶',previewBody,[4.5,4.5,8],[11.5,11.5,8]);plane('仅预览：闪电',previewFx,[4.5,4.5,7.99],[11.5,11.5,7.99]);
        metal.select();Canvas.updateAll();
        Preview.selected.loadAnglePreset({position:[32,24,48],target:[8,8,8],projection:'perspective'});
        saveProject('overload_simulation_chamber_hollow');
        globalThis.overloadVisuals={perfectTex:previewBody,fxTex:previewFx,save:()=>saveProject('overload_simulation_chamber_hollow')};
        writeText('art/visuals-export.json',{source:'Blockbench local workshop',spriteSize:16,lightningFrames:12,bodyLayers:1,effectLayers:1,chamberElements:model.elements.length,glassPanels:6});
        await pause();Screencam.screenshotPreview(Preview.selected,{width:640,height:640},url=>Blockbench.writeFile(ROOT+'/art/chamber_preview.png',{savetype:'image',content:url}));
        Blockbench.showQuickMessage('水晶、闪电图层与六面玻璃模拟室已导出',6000);
    }
    Plugin.register('overload_sim_visuals',{
        title:'Overload Simulation Visual Workshop',author:'Codex',description:'Layered crystals, lightning flipbook and a chamber with six glass panels.',icon:'bolt',version:'0.3.0',variant:'desktop',
        onload(){
            eval(require('fs').readFileSync(ROOT+'/art/multiblock_simulation_workshop.js','utf8'));
            multiblockAction=new Action('overload_sim_draw_multiblock',{name:'绘制多方块模拟室',icon:'view_in_ar',click(){globalThis.drawMultiblockSimulation().catch(error=>{writeText('art/multiblock-error.txt',String(error.stack||error));Blockbench.showMessageBox({title:'多方块素材导出错误',message:String(error.stack||error)});});}});
            MenuBar.addAction(multiblockAction,'tools');
            generateAction=new Action('overload_sim_draw_visuals',{name:'绘制模拟水晶与镂空模拟室',icon:'bolt',click(){generate().catch(error=>{writeText('art/visuals-error.txt',String(error.stack||error));Blockbench.showMessageBox({title:'素材导出错误',message:String(error.stack||error)});});}});
            saveAction=new Action('overload_sim_save_visuals',{name:'保存当前模拟室分层工程',icon:'save',click(){globalThis.overloadVisuals?.save();}});
            previewAction=new Action('overload_sim_preview_visuals',{name:'导出模拟室预览图',icon:'photo_camera',click(){Screencam.screenshotPreview(Preview.selected,{width:640,height:640},url=>Blockbench.writeFile(ROOT+'/art/chamber_preview.png',{savetype:'image',content:url}));}});
            MenuBar.addAction(generateAction,'tools');MenuBar.addAction(saveAction,'tools');MenuBar.addAction(previewAction,'tools');
        },onunload(){generateAction?.delete();saveAction?.delete();previewAction?.delete();multiblockAction?.delete();}
    });
})();
