// Original pixel atlas painted/exported inside Blockbench. MIT; qiqi and contributors.
// Uses the established chamber/coil palette. Reused crystal sprites keep their own license.
(function () {
    let action;
    async function draw(ROOT) {
        const c = document.createElement('canvas'); c.width = c.height = 128;
        const g = c.getContext('2d'); g.imageSmoothingEnabled = false;
        const colors = { white:'#eeeaf2', light:'#fffafd', silver:'#b7aabd', shade:'#d9cfdf',
            pink:'#edacd3', rose:'#b8749e', glow:'#ffe6f5', dark:'#34303f', black:'#15121c', blue:'#bbeeff' };
        function rect(x,y,w,h,color) { g.fillStyle = colors[color] || color; g.fillRect(x,y,w,h); }
        function tile(i, fn) { g.save(); g.translate(i%8*16, Math.floor(i/8)*16); fn(); g.restore(); }
        function panel(color='white') {
            rect(0,0,16,16,'silver'); rect(1,1,14,14,color);
            rect(1,1,14,1,'light'); rect(1,1,1,14,'light'); rect(2,14,13,1,'shade');
            [[2,2],[12,2],[2,12],[12,12]].forEach(([x,y])=>{rect(x,y,2,2,'rose');rect(x,y,1,1,'glow');});
        }
        tile(0,()=>{panel();rect(4,4,8,8,'shade');rect(5,5,6,6,'white');});
        tile(1,()=>{panel();for(let y=4;y<13;y+=3){rect(4,y,8,1,'dark');rect(4,y+1,8,1,'shade');}});
        tile(2,()=>{panel();rect(4,3,2,10,'rose');rect(6,3,1,10,'pink');rect(9,3,2,10,'pink');rect(11,3,1,10,'glow');});
        tile(3,()=>{rect(0,0,16,16,'dark');rect(1,1,14,14,'black');for(let y=3;y<14;y+=3){rect(2,y,12,1,'#59485f');rect(2,y+1,12,1,'#282330');}});
        tile(4,()=>{rect(0,0,16,16,'rgba(248,224,239,.10)');rect(3,3,2,1,'rgba(255,250,253,.13)');rect(4,4,1,2,'rgba(255,250,253,.13)');});
        tile(5,()=>rect(0,0,16,16,'white'));
        tile(6,()=>rect(0,0,16,16,'pink'));
        tile(7,()=>rect(0,0,16,16,'dark'));
        // Eight related pixel glyphs: emitter, constraint, enhancement, burst,
        // deflector, focus, core and receiver. Every stroke is an integer pixel.
        const pixels = [
            [[9,3],[8,4],[7,5],[6,6],[7,6],[8,6],[9,6],[8,7],[7,8],[6,9],[7,9],[8,9],[7,10],[6,11],[5,12]],
            [[4,4],[5,4],[4,5],[10,4],[11,4],[11,5],[4,10],[4,11],[5,11],[11,10],[11,11],[10,11],[7,7],[8,7],[7,8],[8,8]],
            [[4,5],[5,6],[6,7],[5,8],[4,9],[7,5],[8,6],[9,7],[8,8],[7,9],[11,10],[11,11],[11,12],[10,11],[12,11]],
            [[7,3],[8,3],[7,4],[8,4],[4,5],[5,6],[10,6],[11,5],[3,7],[4,7],[11,7],[12,7],[6,7],[7,7],[8,7],[9,7],[6,8],[7,8],[8,8],[9,8],[4,10],[5,9],[10,9],[11,10],[7,11],[8,11],[7,12],[8,12]],
            [[7,3],[6,4],[8,4],[7,4],[7,5],[3,7],[4,6],[4,8],[4,7],[5,7],[6,7],[7,7],[8,7],[9,7],[10,7],[11,7],[12,7],[11,6],[11,8],[7,8],[7,9],[7,10],[7,11],[6,11],[8,11],[7,12]],
            [[3,4],[4,5],[5,6],[6,7],[7,7],[8,7],[9,7],[10,7],[11,7],[12,7],[3,11],[4,10],[5,9],[6,8],[7,8],[8,8],[9,8],[10,8],[11,8],[12,8]],
            [[7,4],[8,4],[5,5],[10,5],[4,6],[11,6],[4,7],[11,7],[4,8],[11,8],[5,10],[10,10],[7,11],[8,11],[7,7],[8,7],[7,8],[8,8]],
            [[6,3],[7,3],[8,3],[9,3],[4,4],[11,4],[3,6],[12,6],[3,8],[12,8],[4,10],[11,10],[6,12],[7,12],[8,12],[9,12],[6,6],[9,5],[8,8],[5,9],[10,10]]
        ];
        pixels.forEach((points,i)=>tile(8+i,()=>{
            rect(0,0,16,16,'black');rect(2,2,12,12,'#241c2c');
            points.forEach(([x,y])=>rect(x,y,1,1,'pink'));
            points.slice(0,3).forEach(([x,y])=>rect(x,y,1,1,'glow'));
        }));
        tile(16,()=>{panel();rect(3,6,10,4,'shade');rect(3,6,10,1,'light');rect(3,9,10,1,'silver');});
        tile(17,()=>{rect(0,0,16,16,'white');rect(0,4,16,1,'rose');rect(0,5,16,2,'pink');rect(0,7,16,1,'glow');rect(0,11,16,1,'shade');});
        tile(18,()=>{panel('dark');for(let y=4;y<13;y+=2)rect(3,y,10,1,'black');rect(3,3,10,1,'pink');});
        tile(19,()=>{rect(0,0,16,16,'rose');for(let x=0;x<16;x+=4){rect(x,0,2,16,'pink');rect(x,0,1,16,'glow');}});
        tile(20,()=>{rect(0,0,16,16,'rgba(248,224,239,.10)');rect(0,0,16,1,'pink');rect(0,15,16,1,'pink');});
        tile(21,()=>{rect(0,0,16,16,'rose');rect(1,1,14,14,'pink');rect(2,2,12,12,'black');rect(3,12,10,1,'#704761');});
        tile(22,()=>{rect(0,0,16,16,'dark');rect(3,3,10,10,'#477d92');rect(4,4,8,8,'blue');rect(5,5,2,6,'light');});
        tile(23,()=>{panel();rect(4,3,8,10,'dark');rect(3,4,10,8,'dark');rect(5,4,6,8,'pink');rect(4,5,8,6,'pink');rect(6,5,4,6,'black');rect(5,6,6,4,'black');});
        newProject(Formats.java_block); Project.name='Laser pixel atlas';
        Project.texture_width=Project.texture_height=128; Project.ambientocclusion=false;
        const t=new Texture({name:'laser_pixel_atlas.png',namespace:'overload_sim',folder:'block',
            width:128,height:128,uv_width:128,uv_height:128}).fromDataURL(c.toDataURL()).add();
        await new Promise(r=>setTimeout(r,150));
        t.layers_enabled=true;t.layers.length=0;
        t.layers.push(new TextureLayer({name:'粉白金属 · 导光槽 · 八种设备图标 · 透明观察玻璃',
            width:128,height:128,image_data:g.getImageData(0,0,128,128),opacity:100,visible:true,blend_mode:'default'},t));
        t.selected_layer=t.layers[0];t.updateLayerChanges(true);
        const path=ROOT+'/src/main/resources/assets/overload_sim/textures/block/laser_pixel_atlas.png';
        t.path=path;Blockbench.writeFile(path,{savetype:'image',content:c.toDataURL()});t.saved=true;
        const cube=new Cube({name:'图集预览',from:[0,0,0],to:[16,16,16],box_uv:false}).init();
        for(const f of Object.values(cube.faces)){f.texture=t.uuid;f.uv=[0,0,128,128];}
        Canvas.updateAll();
        Project.save_path=ROOT+'/art/blockbench/laser-pixel-atlas.bbmodel';
        Blockbench.writeFile(Project.save_path,{savetype:'text',content:Codecs.project.compile()});Project.saved=true;
        Blockbench.showQuickMessage('激光像素图集已绘制并导出到开发工程',5000);
    }
    Plugin.register('laser_art_workshop',{
        title:'模拟：激光像素绘制',author:'qiqi',description:'粉白激光设备的原生 Blockbench 绘制与导出',
        icon:'bolt',version:'0.2.0',variant:'desktop',
        onload(){action=new Action('overload_sim_laser_draw',{name:'绘制激光粉白像素图集',icon:'bolt',click(){
            new Dialog({id:'laser_art_root',title:'激光像素图集',form:{root:{label:'模组工程目录',type:'text',value:'D:/MinecraftDev/OverloadSimulation'}},
                onConfirm(values){this.hide();draw(values.root.replace(/\\/g,'/')).catch(e=>Blockbench.showMessageBox({title:'绘制失败',message:String(e.stack||e)}));}}).show();
        }});MenuBar.addAction(action,'tools');},
        onunload(){action?.delete();}
    });
})();
