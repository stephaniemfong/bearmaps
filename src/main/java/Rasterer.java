import java.util.HashMap;
import java.util.Map;

/**
 * This class provides all code necessary to take a query box and produce
 * a query result. The getMapRaster method must return a Map containing all
 * seven of the required fields, otherwise the front end code will probably
 * not draw the output correctly.
 */
public class Rasterer {
    private double zeroULLon;
    private double zeroLRLon;
    private double zeroULLat;
    private double zeroLRLat;

    public Rasterer() {
        MapServer ms = new MapServer();
        zeroULLon = ms.ROOT_ULLON;
        zeroLRLon = ms.ROOT_LRLON;
        zeroULLat = ms.ROOT_ULLAT;
        zeroLRLat = ms.ROOT_LRLAT;
    }

    /**
     * Takes a user query and finds the grid of images that best matches the query. These
     * images will be combined into one big image (rastered) by the front end. <br>
     *
     *     The grid of images must obey the following properties, where image in the
     *     grid is referred to as a "tile".
     *     <ul>
     *         <li>The tiles collected must cover the most longitudinal distance per pixel
     *         (LonDPP) possible, while still covering less than or equal to the amount of
     *         longitudinal distance per pixel in the query box for the user viewport size. </li>
     *         <li>Contains all tiles that intersect the query bounding box that fulfill the
     *         above condition.</li>
     *         <li>The tiles must be arranged in-order to reconstruct the full image.</li>
     *     </ul>
     *
     * @param params Map of the HTTP GET request's query parameters - the query box and
     *               the user viewport width and height.
     *
     * @return A map of results for the front end as specified: <br>
     * "render_grid"   : String[][], the files to display. <br>
     * "raster_ul_lon" : Number, the bounding upper left longitude of the rastered image. <br>
     * "raster_ul_lat" : Number, the bounding upper left latitude of the rastered image. <br>
     * "raster_lr_lon" : Number, the bounding lower right longitude of the rastered image. <br>
     * "raster_lr_lat" : Number, the bounding lower right latitude of the rastered image. <br>
     * "depth"         : Number, the depth of the nodes of the rastered image <br>
     * "query_success" : Boolean, whether the query was able to successfully complete; don't
     *                    forget to set this to true on success! <br>
     */
    public Map<String, Object> getMapRaster(Map<String, Double> params) {
        System.out.println(params);
        Map<String, Object> results = new HashMap<>();

        double mapLonDPP = calcDPP(zeroULLon, zeroLRLon, 256.0);
        double mapLatDPP = calcDPP(zeroULLat, zeroLRLat, 256.0);
        double goalLonDPP = calcDPP(params.get("ullon"), params.get("lrlon"), params.get("w"));
        double fraction = mapLonDPP / goalLonDPP;
        double depth = (Math.log(fraction) / Math.log(2)) + 1; //log base 2
        int d = (int) depth;
        if (d > 7) {
            d = 7;
        }

        double lonDPP = mapLonDPP / Math.pow(2, d);
        double pixDistLon = lonDPP * 256;
        double latDPP = mapLatDPP / Math.pow(2, d);
        double pixDistLat = latDPP * 256;

        double ulXNum = Math.ceil((params.get("ullon") - zeroULLon) / pixDistLon) - 1;
        if (params.get("ullon") < zeroULLon) {
            ulXNum = 0;
        }
        double ullon = zeroULLon + (ulXNum * pixDistLon);

        double lrXNum = Math.ceil((params.get("lrlon") - zeroULLon) / pixDistLon);
        if (params.get("lrlon") > zeroLRLon) {
            lrXNum = ((int) Math.pow(2, d)) - 1;
        }
        double lrlon = zeroULLon + (lrXNum * pixDistLon);

        double ulYNum = Math.ceil((params.get("ullat") - zeroULLat) / pixDistLat) - 1;
        if (params.get("ullat") > zeroULLat) {
            ulYNum = 0;
        }
        double ullat = zeroULLat + (ulYNum * pixDistLat);

        double lrYNum = Math.ceil((params.get("lrlat") - zeroULLat) / pixDistLat);
        if (params.get("lrlat") < zeroLRLat) {
            lrYNum = ((int) Math.pow(2, d)) - 1;
        }
        double lrlat = zeroULLat + (lrYNum * pixDistLat);

        int width = (int) (lrXNum - ulXNum);
        int height = (int) (lrYNum - ulYNum);
        String[][] rGrid = new String[height][width];

        int y = (int) ulYNum;
        for (int i = 0; i < height; i++) {
            int x = (int) ulXNum;
            for (int j = 0; j < width; j++) {
                rGrid[i][j] = "d" + d + "_x" + x + "_y" + y + ".png";
                x++;
            }
            y++;
        }

        results.put("render_grid", rGrid);
        results.put("raster_ul_lon", ullon);
        results.put("raster_ul_lat", ullat);
        results.put("raster_lr_lon", lrlon);
        results.put("raster_lr_lat", lrlat);
        results.put("depth", d);
        results.put("query_success", true);
//        if (params.get("ullon") < zeroLRLon
//                || params.get("ullat") < zeroLRLat) {
//            System.out.println("hit false");
//            results.put("query_success", false);
//        }
        return results;
    }

    /** calculates lonDPP in lon/pixel */
    private double calcDPP(double ul, double lr, double width) {
        double xDist = lr - ul;
        return xDist / width;
    }
}
