import java.io.BufferedReader;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by User on 15.01.15.
 */
public class GoodsReader {

    private static int MAX_GOODS_INDEX = 516000;
    private int stopPoint;
    private float marginPrice;
    private Path goodsFile;
    private Path skippedListFile;
    private List<Integer> skippedList;
    private ArrayList<String> goodsList;

    public GoodsReader() {
        this("C_goods.dbf");
    }

    public GoodsReader(String fileName) {
        this(fileName, (float) 0.05);
    }

    public GoodsReader(String fileName, float marginPrice) {
        this(fileName, marginPrice, MAX_GOODS_INDEX);
    }

    public GoodsReader(String fileName, float marginPrice, int stopPoint) {
        this(fileName, marginPrice, stopPoint, "");
    }

    public GoodsReader(String goodsFileName, float marginPrice, int stopPoint, String skippedListfile) {
        this.goodsFile = Paths.get(goodsFileName);
        this.marginPrice = marginPrice;
        this.skippedListFile = Paths.get(skippedListfile);

        if (stopPoint != MAX_GOODS_INDEX) {
            this.stopPoint = stopPoint * 129;
        } else {
            this.stopPoint = stopPoint;
        }

        System.out.println(System.getProperty("user.dir"));
        if (!Files.exists(this.goodsFile)) {
            System.out.println("Goods file does not exists in directory: " + System.getProperty("user.dir") + "\\");
        }
        if (!this.goodsFile.getFileName().startsWith("C_goods.dbf")) {
            System.out.println("File does not match");
        }

        this.skippedList = new ArrayList<>();
    }

    /*
     * For C_goods.dbf charset must be StandardCharsets.ISO_8859_1
     */
    public ArrayList<String> readFromFile(Path path, Charset cs) throws NullPointerException, IOException, IllegalStateException {
        if (path == null) {
            throw new NullPointerException();
        }

        if (path.toString().isEmpty()) {
            throw new IllegalStateException();
        }

        ArrayList<String> lines = new ArrayList<>();
        try {
            String line;
            BufferedReader reader = Files.newBufferedReader(path, cs);
            while ((line = reader.readLine()) != null) {
                lines.add(line);
            }
            reader.close();
        } catch (IOException e) {
            e.printStackTrace();
            System.out.println("Cannot read the line");
            throw e;
        }

        if (lines.size() == 0) {
            System.out.println("File is empty");
        }
        return lines;
    }

    public ArrayList<Double> parseGoodsPrice() {
        ArrayList<Double> pricesList = new ArrayList<>();

        String mainLine = goodsList.get(5);
        if (mainLine.equals("")) {
            System.out.println("Line is empty");
        }
        for (int i = 35; i < mainLine.length(); i = i + 129) {
            if (i >= stopPoint) break;
            String s = mainLine.substring(i, i + 5);
            pricesList.add(Double.parseDouble(s));
        }
        return pricesList;
    }

    public void parseSkipperList(List<String> skippedFileContent) throws NumberFormatException {
        for (String line : skippedFileContent) {
            char[] charLine = line.toCharArray();
            if (charLine.length == 1) {
                if (charLine[0] != '\n')
                    addToSkipList(Integer.parseInt(String.valueOf(charLine)));
                continue;
            }

            if (charLine.length == 0) {
                continue;
            }

            boolean isDefis = false;
            int prevValue = 0;
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < charLine.length + 1; i++) {
                if (i >= charLine.length) {
                    if (isDefis) {
                        int lastValue = Integer.parseInt(sb.toString());
                        for (int c = prevValue+1; c <= lastValue; c++) {
                            skippedList.add(c);
                        }
                    } else {
                        skippedList.add(Integer.parseInt(sb.toString()));
                    }
                    break;
                } else if (charLine[i] == '-') {
                    prevValue = Integer.parseInt(sb.toString().trim());
                    skippedList.add(prevValue);
                    sb.setLength(0);
                    sb.trimToSize();
                    isDefis = true;
                } else if (charLine[i] == ' ' || charLine[i] == ';' || charLine[i] == ',') {
                    if(isDefis){
                        int lastValue = Integer.parseInt(sb.toString().trim());
                        for (int c = prevValue+1; c <= lastValue; c++) {
                            skippedList.add(c);
                        }
                        sb.setLength(0);
                        sb.trimToSize();
                        isDefis = false;
                    }else{
                        skippedList.add(Integer.parseInt(sb.toString().trim()));
                        sb.setLength(0);
                        sb.trimToSize();
                    }
                } else sb.append(charLine[i]);
            }
        }
    }

    /*
        @param prices - List with number of items, which must bee skipped. These items didn't need to be upgraded.
        @return array list with new prices
     */
    public ArrayList<Double> marginPrices(ArrayList<Double> prices) {
        ArrayList<Double> newPrices = new ArrayList<>(prices.size());
        for (int i = 0; i < prices.size(); i++) {

            boolean isSkipped = false;
            for (Integer aSkipped : skippedList) {
                if (i == aSkipped - 1) {
                    newPrices.add(i, prices.get(i));
                    isSkipped = true;
                }
            }
            if (isSkipped) continue;

            Double ff = Math.round((prices.get(i) + (prices.get(i) * getMarginPrice())) * 100.0) / 100.0;
            newPrices.add(i, ff);
        }
        return newPrices;
    }

    /*

     */
    public void savePrices(ArrayList<Double> prices) {
        ArrayList<String> pricesTextList = new ArrayList<>();
        for (Double d : prices) {
            String s = d.toString();
            if (s.charAt(1) == '.') {
                s = " " + s;
            } else if (s.charAt(2) == '.' && s.length() == 4) {
                s = s + "0";
            }
            pricesTextList.add(s);
        }

        char[] chars = goodsList.get(5).toCharArray();
        int goodsCounter = 0;
        for (int i = 35; i <= chars.length; i = i + 129) {
            if (goodsCounter == prices.size()) break;
            char[] priceChars = pricesTextList.get(goodsCounter).toCharArray();
            for (int j = 0; j < priceChars.length; j++) {
                chars[i + j] = priceChars[j];
            }
            goodsCounter++;
        }
        goodsList.add(5, new String(chars));
    }

    public void saveNewGoods() {
        String path = "new_C_goods.dbf";
        try {
            FileChannel fc = new FileOutputStream(path).getChannel();
            for (String line : goodsList) {
                line = line + "\n";
                fc.write(ByteBuffer.wrap(line.getBytes(StandardCharsets.ISO_8859_1)));
            }
            fc.close();
            System.out.println("Saved");
        } catch (IOException e) {
            System.out.println("Cannot save the file");
            e.printStackTrace();
        }

    }


    public Path getGoodsFile() {
        return goodsFile;
    }

    public void setGoodsFile(Path goodsFile) {
        this.goodsFile = goodsFile;
    }

    public List<Integer> getSkippedList() {
        return skippedList;
    }

    public void setSkippedList(List<Integer> skippedList) {
        this.skippedList = skippedList;
    }

    public void addToSkipList(int i) {
        skippedList.add(i);
    }

    public float getMarginPrice() {
        return marginPrice;
    }

    public void setMarginPrice(float marginPrice) {
        this.marginPrice = marginPrice;
    }

    public int getStopPoint() {
        return stopPoint;
    }

    public void setStopPoint(int stopPoint) {
        this.stopPoint = stopPoint;
    }

    public Path getSkippedListPath() {
        return skippedListFile;
    }

    public void setSkippedListFile(Path skippedListFile) {
        this.skippedListFile = skippedListFile;
    }

    public ArrayList<String> getGoodsList() {
        return goodsList;
    }

    public void setGoodsList(ArrayList<String> goodsList) {
        this.goodsList = goodsList;
    }

    public static void main(String[] args) {
        GoodsReader gr = null;

        if (args != null) {
            switch (args.length) {
                case 1:
                    gr = new GoodsReader(args[0]); //read *.dbf as first argument
                    break;
                case 2:
                    gr = new GoodsReader(args[0],
                            Float.parseFloat(args[1])); //margin
                    break;
                case 3:
                    gr = new GoodsReader(args[0],
                            Float.parseFloat(args[1]),
                            Integer.parseInt(args[2])); //stopPoint
                    break;
                case 4:
                    gr = new GoodsReader(args[0],
                            Float.parseFloat(args[1]),
                            Integer.parseInt(args[2]),
                            args[3]);                    //skippedFile
                    break;
                default:
                    gr = new GoodsReader();
            }
        } else {
            System.out.println("Wrong arguments. Exit");
            return;
        }

        try {
            gr.setGoodsList(
                    gr.readFromFile(gr.getGoodsFile(),
                            StandardCharsets.ISO_8859_1));
        } catch (NullPointerException e) {
            System.out.println("Path is nullable. Cannot read file");
            return;
        } catch (IOException e) {
            System.out.println("Cannot read from file");
            return;
        }

        try {
            gr.parseSkipperList(
                    gr.readFromFile(
                            gr.getSkippedListPath(), StandardCharsets.UTF_8));
        } catch (IllegalStateException ise) {
            System.out.println("** Skipped file is empty.");
            gr.setSkippedList(new ArrayList<>());
        } catch (NullPointerException e) {
            System.out.println("Cannot read skipped list file. Will try without him");
            gr.setSkippedList(new ArrayList<>());
        } catch (IOException e) {
            System.out.println("Cannot read from file");
            return;
        }

        ArrayList<Double> priceList = gr.parseGoodsPrice();

        ArrayList<Double> newPriceList = gr.marginPrices(priceList);
        gr.savePrices(newPriceList);
        gr.saveNewGoods();
    }
}
