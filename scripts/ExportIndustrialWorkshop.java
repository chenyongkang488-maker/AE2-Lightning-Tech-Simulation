import dev.overloadsim.core.IndustrialMachineMesh;
import dev.overloadsim.core.ReceiverShellMesh;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

/** Exports the actual immutable renderer meshes into an editable Blockbench project.
 * Run in Java source-file mode after compilation; it does not draw or edit pixels. */
class ExportIndustrialWorkshop {
    private static final List<String> elements = new ArrayList<>();
    private static final List<String> groups = new ArrayList<>();
    private static int sequence;
    public static void main(String[] args) throws Exception {
        Path root = Path.of(args.length == 0 ? "." : args[0]).toAbsolutePath().normalize();
        group("01 · 分段装甲骨架", () -> {
            var surfaces = new ArrayList<IndustrialMachineMesh.Surface>();
            for (int axis = 0; axis < 3; axis++) {
                surfaces.addAll(IndustrialMachineMesh.armouredRing(axis, 3.3));
            }
            // Leave the true upstream optical-port block visible, just as the renderer does.
            for (var face : IndustrialMachineMesh.withoutCell(surfaces,0,0,-3)) mesh(face.quad(), face.tile(), false);
        });
        group("02 · 双层机械底座", () -> {
            for (var face : IndustrialMachineMesh.receiverPlinth()) mesh(face.quad(), face.tile(), false);
        });
        group("03 · 淡粉透明球壳", () -> {
            var surfaces = new ArrayList<IndustrialMachineMesh.Surface>();
            for (var face : ReceiverShellMesh.glass()) surfaces.add(new IndustrialMachineMesh.Surface(face,4,false));
            for (var face : IndustrialMachineMesh.withoutCell(surfaces,0,0,-3)) mesh(face.quad(),4,true);
        });
        byte[] atlas = Files.readAllBytes(root.resolve("src/main/resources/assets/overload_sim/textures/block/laser_pixel_atlas.png"));
        String texture = "{\"name\":\"laser_pixel_atlas.png\",\"id\":\"0\",\"width\":128,\"height\":128,\"uv_width\":128,\"uv_height\":128,"
                + "\"relative_path\":\"../../src/main/resources/assets/overload_sim/textures/block/laser_pixel_atlas.png\","
                + "\"visible\":true,\"saved\":true,\"source\":\"data:image/png;base64," + Base64.getEncoder().encodeToString(atlas) + "\"}";
        String project = "{\"meta\":{\"format_version\":\"5.0\",\"model_format\":\"free\",\"box_uv\":false},"
                + "\"name\":\"Industrial particle constrainer\",\"resolution\":{\"width\":128,\"height\":128},"
                + "\"credit\":\"qiqi · exact Java renderer mesh; pixel atlas authored in native Blockbench. Controller/port remain ordinary interactive blocks in game.\","
                + "\"elements\":[" + String.join(",", elements) + "],\"outliner\":[" + String.join(",", groups) + "],\"textures\":[" + texture + "]}";
        Path output = root.resolve("art/blockbench/particle-constrainer-formed.bbmodel");
        Files.writeString(output, project + "\n");
        System.out.println("Exported " + elements.size() + " exact renderer faces in " + groups.size() + " editable groups: " + output);
    }
    private static void group(String name, Runnable author) {
        int start = sequence; author.run();
        var ids = new ArrayList<String>();
        for (int i = start; i < sequence; i++) ids.add("\"" + id("face-" + i) + "\"");
        groups.add("{\"name\":\"" + name + "\",\"origin\":[0,0,0],\"uuid\":\"" + id("group-" + groups.size())
                + "\",\"isOpen\":false,\"visibility\":true,\"children\":[" + String.join(",", ids) + "]}");
    }
    private static void mesh(ReceiverShellMesh.Quad face, int tile, boolean glass) {
        var points = List.of(face.a(),face.b(),face.c(),face.d());
        var unique = new LinkedHashMap<String, Integer>();
        var vertices = new ArrayList<String>();
        var indices = new ArrayList<Integer>();
        for (var p : points) {
            String coordinates = p.x()*16 + "," + p.y()*16 + "," + p.z()*16;
            Integer index = unique.get(coordinates);
            if (index == null) { index = unique.size(); unique.put(coordinates,index); vertices.add("\"v" + index + "\":[" + coordinates + "]"); }
            if (!indices.contains(index)) indices.add(index);
        }
        if (indices.size() < 3) return;
        int number = sequence++;
        double x = glass ? 66.5 : (tile%8)*16+.5, y = glass ? 2.5 : (tile/8)*16+.5, width = glass ? 11 : 15;
        var uv = new ArrayList<String>();
        for (int i = 0; i < points.size(); i++) {
            var p = points.get(i);
            String coordinates = p.x()*16 + "," + p.y()*16 + "," + p.z()*16;
            int index = unique.get(coordinates);
            if (uv.stream().noneMatch(s -> s.startsWith("\"v"+index+"\":")))
                uv.add("\"v"+index+"\":["+(x+p.u()*width)+","+(y+p.v()*width)+"]");
        }
        String order = String.join(",", indices.stream().map(i -> "\"v"+i+"\"").toList());
        elements.add("{\"name\":\""+(glass ? "玻璃" : "机械饰面")+" · "+number+"\",\"type\":\"mesh\",\"uuid\":\""+id("face-"+number)
                +"\",\"origin\":[0,0,0],\"rotation\":[0,0,0],\"visibility\":true,\"vertices\":{"+String.join(",",vertices)
                +"},\"faces\":{\"panel\":{\"vertices\":["+order+"],\"uv\":{"+String.join(",",uv)+"},\"texture\":0}}}");
    }
    private static String id(String value) {
        return UUID.nameUUIDFromBytes(("overload_sim:industrial/"+value).getBytes(StandardCharsets.UTF_8)).toString();
    }
}
