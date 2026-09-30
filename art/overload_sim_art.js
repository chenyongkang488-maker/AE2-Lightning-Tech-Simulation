// Load this local plugin in Blockbench: File > Plugins > Load from file.
// All textures are drawn and exported inside Blockbench, using its Texture/Codec APIs.
(function(){
    let action;
    const palette={outline:'#8a537b',shadow:'#bf77a7',rose:'#eb95c1',pink:'#ffc1dc',ice:'#ffe7f3',white:'#fff9fd',metal:'#d8c8d7'};
    function crystal(stage){
        const canvas=document.createElement('canvas');canvas.width=canvas.height=32;
        const ctx=canvas.getContext('2d');ctx.imageSmoothingEnabled=false;
        function px(x,y,c){ctx.fillStyle=c;ctx.fillRect(x,y,1,1);}
        function inside(x,y){return y>=3&&y<=27&&Math.abs(x-15.5)<=Math.min((y-2)*.8,(28-y)*.7,8);}
        for(let y=2;y<29;y++)for(let x=5;x<27;x++)if(inside(x,y)){
            let edge=!inside(x-1,y)||!inside(x+1,y)||!inside(x,y-1)||!inside(x,y+1);
            let c=edge?palette.outline:x<12?palette.white:x<16?palette.ice:x<20?palette.pink:palette.shadow;
            if(stage===0&&!edge)c=x<13?palette.white:x<19?palette.ice:palette.metal;
            if(stage===2&&!edge)c=x<13?palette.white:x<18?palette.pink:palette.rose;
            px(x,y,c);
        }
        // White metal cradle and pink rim.
        for(let x=9;x<=22;x++){px(x,22,palette.shadow);px(x,23,palette.white);px(x,24,palette.ice);}
        for(let y=19;y<=23;y++){px(8,y,palette.outline);px(9,y,palette.pink);px(22,y,palette.pink);px(23,y,palette.outline);}
        const bolt=[[17,7],[16,8],[15,9],[14,10],[13,11],[14,12],[15,12],[16,12],[15,13],[14,14],[13,15],[12,16],[13,16],[14,15],[15,14],[16,13],[17,12],[18,11],[17,10],[16,10],[15,10],[16,9],[17,8]];
        for(const[x,y]of bolt)px(x,y,stage===0?palette.metal:palette.white);
        if(stage===1){px(11,8,palette.white);px(20,18,palette.white);}
        if(stage===2){
            const spark=(x,y)=>{px(x,y,palette.white);px(x-1,y,palette.pink);px(x+1,y,palette.pink);px(x,y-1,palette.pink);px(x,y+1,palette.pink);};
            spark(5,10);spark(26,18);spark(24,5);
            for(let x=12;x<20;x++){px(x,1,palette.pink);px(x,29,palette.white);}
            for(let y=11;y<20;y++){px(4,y,palette.pink);px(27,y,palette.pink);}
        }
        return canvas;
    }
    function chamber(){
        const canvas=document.createElement('canvas');canvas.width=canvas.height=32;const c=canvas.getContext('2d');
        const rect=(x,y,w,h,color)=>{c.fillStyle=color;c.fillRect(x,y,w,h);};
        rect(0,0,32,32,palette.outline);rect(1,1,30,30,palette.metal);rect(2,2,28,28,palette.white);rect(3,3,26,26,palette.ice);
        rect(5,5,22,22,palette.shadow);rect(6,6,20,20,palette.rose);rect(7,7,18,18,palette.pink);rect(9,9,14,14,palette.outline);rect(10,10,12,12,'#624064');rect(11,11,10,10,'#a76ba2');
        const core=crystal(2);c.drawImage(core,8,4,16,24);
        for(const[x,y]of [[2,2],[27,2],[2,27],[27,27]]){rect(x,y,3,3,palette.outline);rect(x,y,2,2,palette.white);}
        for(let n=0;n<4;n++){rect(7+n*5,2,3,2,palette.rose);rect(7+n*5,28,3,2,palette.rose);}
        rect(2,13,2,6,palette.pink);rect(28,13,2,6,palette.pink);
        return canvas;
    }
    async function generate(root){
        newProject(Formats.free);
        Project.name='overload_sim_pink_white';Project.texture_width=32;Project.texture_height=32;
        const names=['blank_simulation_crystal','simulation_crystal','perfect_simulation_crystal','overload_simulation_chamber'];
        const textures=[];
        for(let i=0;i<names.length;i++){
            const canvas=i===3?chamber():crystal(i);const source=canvas.toDataURL('image/png');
            const tex=new Texture({name:names[i]+'.png',namespace:'overload_sim',folder:i===3?'block':'item',width:32,height:32}).fromDataURL(source).add();
            textures.push(tex);
            const path=root+'/src/main/resources/assets/overload_sim/textures/'+(i===3?'block/':'item/')+names[i]+'.png';
            Blockbench.writeFile(path,{savetype:'image',content:source});tex.path=path;tex.saved=true;
        }
        await new Promise(resolve=>setTimeout(resolve,300));
        const cube=new Cube({name:'chamber_preview',from:[0,0,0],to:[16,16,16]}).init();
        for(const face of Object.values(cube.faces)){face.texture=textures[3].uuid;face.uv=[0,0,32,32];}
        textures[2].select();Canvas.updateAll();
        Blockbench.writeFile(root+'/art/overload_sim_pink_white.bbmodel',{savetype:'text',content:Codecs.project.compile()});
        Project.save_path=root+'/art/overload_sim_pink_white.bbmodel';Project.saved=true;
        Blockbench.showMessageBox({title:'粉白素材已导出',message:'已在 Blockbench 内生成三种水晶与模拟室贴图，并保存可编辑 .bbmodel 工程。可在左侧纹理中选择水晶继续绘制。'});
    }
    Plugin.register('overload_sim_art',{
        title:'Overload Simulation Art',author:'Codex',description:'Local pink/white texture workshop for the Overload Simulation addon.',icon:'bolt',version:'0.1.0',variant:'desktop',
        onload(){action=new Action('overload_sim_generate_art',{name:'生成过载模拟粉白素材',icon:'bolt',click(){new Dialog({id:'overload_sim_art_options',title:'过载模拟素材',form:{root:{label:'工程目录',type:'text',value:'D:/MinecraftDev/OverloadSimulation'}},onConfirm(values){this.hide();generate(values.root).catch(error=>Blockbench.showMessageBox({title:'素材导出失败',message:String(error.stack||error)}));}}).show();}});MenuBar.addAction(action,'tools');},
        onunload(){action?.delete();}
    });
})();
