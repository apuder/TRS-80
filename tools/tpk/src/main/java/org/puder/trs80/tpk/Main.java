package org.puder.trs80.tpk;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import net.iharder.Base64;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.puder.trs80.tpk.json.Configuration;
import org.puder.trs80.tpk.json.Listing;
import org.puder.trs80.tpk.json.TPK;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

public class Main {

    // General
    final private static String OPTION_NO_ZIP = "no-zip";
    final private static String OPTION_HELP   = "help";

    // Configuration parameters
    final private static String OPTION_NAME  = "name";
    final private static String OPTION_MODEL = "model";
    final private static String OPTION_DISK1 = "disk1";
    final private static String OPTION_DISK2 = "disk2";
    final private static String OPTION_DISK3 = "disk3";
    final private static String OPTION_DISK4 = "disk4";

    // Listing parameters
    final private static String OPTION_VERSION     = "version";
    final private static String OPTION_PRICE       = "price";
    final private static String OPTION_AUTHOR      = "author";
    final private static String OPTION_EMAIL       = "email";
    final private static String OPTION_SCREENSHOT  = "screenshot";
    final private static String OPTION_DESCRIPTION = "description";


    class CLIException extends Exception {
        public CLIException(String message) {
            super(message);
        }

    }


    private Options createCLIOptions() {
        Options options = new Options();

        // General
        options.addOption(Option.builder("h").longOpt(OPTION_HELP).desc("Help").build());
        options.addOption(
                Option.builder().longOpt(OPTION_NO_ZIP).desc("Do not zip the TPK").build());

        // Configuration options
        options.addOption(Option.builder("n").longOpt(OPTION_NAME).required().hasArg()
                .argName("name").desc("Name of the configuration").build());
        options.addOption(Option.builder("m").longOpt(OPTION_MODEL).required().hasArg()
                .argName("model").desc("TRS-80 model used for emulation [1|3]").build());
        options.addOption(Option.builder("d1").longOpt(OPTION_DISK1).hasArg().argName("file")
                .desc("Path of disk 1 image").build());
        options.addOption(Option.builder("d2").longOpt(OPTION_DISK2).hasArg().argName("file")
                .desc("Path of disk 2 image").build());
        options.addOption(Option.builder("d3").longOpt(OPTION_DISK3).hasArg().argName("file")
                .desc("Path of disk 3 image").build());
        options.addOption(Option.builder("d4").longOpt(OPTION_DISK4).hasArg().argName("file")
                .desc("Path of disk 4 image").build());

        // Listing options
        options.addOption(Option.builder("v").longOpt(OPTION_VERSION).hasArg().argName("version")
                .desc("Version number [integer]").build());
        options.addOption(Option.builder("p").longOpt(OPTION_PRICE).hasArg().argName("price")
                .desc("Price in USD").build());
        options.addOption(Option.builder("a").longOpt(OPTION_AUTHOR).hasArg().argName("author")
                .desc("Full author name").build());
        options.addOption(Option.builder("e").longOpt(OPTION_EMAIL).hasArg().argName("email")
                .desc("Email address").build());
        options.addOption(Option.builder("s").longOpt(OPTION_SCREENSHOT).hasArgs()
                .argName("files...").desc("One or more screenshots").build());
        options.addOption(Option.builder("d").longOpt(OPTION_DESCRIPTION).hasArg()
                .desc("Description").build());

        return options;
    }

    private void checkCLI(CommandLine cli) throws CLIException {
        String model = cli.getOptionValue(OPTION_MODEL);
        if (!model.equals("1") && !model.equals("3")) {
            throw new CLIException("-m Model needs to be 1 or 3");
        }
        checkPath(cli.getOptionValue(OPTION_DISK1));
        checkPath(cli.getOptionValue(OPTION_DISK2));
        checkPath(cli.getOptionValue(OPTION_DISK3));
        checkPath(cli.getOptionValue(OPTION_DISK4));

        String[] screenshots = cli.getOptionValues(OPTION_SCREENSHOT);
        for (String screenshot : screenshots) {
            checkPath(screenshot);
        }

        checkLong(cli.getOptionValue(OPTION_VERSION));
        checkDouble(cli.getOptionValue(OPTION_PRICE));
    }

    private void checkPath(String path) throws CLIException {
        if (path != null) {
            if (!new File(path).exists()) {
                throw new CLIException("Path '" + path + "' does not exist");
            }
        }
    }

    private void checkLong(String n) throws CLIException {
        try {
            if (n != null)
                Long.parseLong(n);
        } catch (NumberFormatException ex) {
            throw new CLIException("'" + n + "' is not an integer");
        }
    }

    private void checkDouble(String n) throws CLIException {
        try {
            if (n != null)
                Double.parseDouble(n);
        } catch (NumberFormatException ex) {
            throw new CLIException("'" + n + "' is not a number");
        }
    }

    private TPK generateTPK(CommandLine cli) {
        TPK tpk = new TPK();
        tpk.setId(UUID.randomUUID().toString());
        tpk.setName(cli.getOptionValue(OPTION_NAME));

        Configuration configuration = new Configuration();
        tpk.setConfiguration(configuration);
        configuration.setModel(cli.getOptionValue(OPTION_MODEL));
        configuration.setDisk1(convertToBase64(cli.getOptionValue(OPTION_DISK1)));
        configuration.setDisk2(convertToBase64(cli.getOptionValue(OPTION_DISK2)));
        configuration.setDisk3(convertToBase64(cli.getOptionValue(OPTION_DISK3)));
        configuration.setDisk4(convertToBase64(cli.getOptionValue(OPTION_DISK4)));

        Listing listing = new Listing();
        tpk.setListing(listing);
        listing.setAuthor(cli.getOptionValue(OPTION_AUTHOR));
        listing.setAuthor_email(cli.getOptionValue(OPTION_EMAIL));
        listing.setVersion(Long.parseLong(cli.getOptionValue(OPTION_VERSION, "1")));
        listing.setPrice(Double.parseDouble(cli.getOptionValue(OPTION_PRICE, "0")));

        String[] screenshots = cli.getOptionValues(OPTION_SCREENSHOT);
        List<String> screenshotsB64 = new ArrayList<>();
        for (String screenshot : screenshots) {
            screenshotsB64.add(convertToBase64(screenshot));
        }
        listing.setScreenshots(screenshotsB64);

        return tpk;
    }

    private void showHelp(Options options) {
        HelpFormatter formatter = new HelpFormatter();
        formatter.printHelp("tpk", options);
    }

    public String convertToBase64(String path) {
        if (path == null) {
            return null;
        }
        String b64 = null;
        try {
            InputStream is = new FileInputStream(new File(path));
            byte[] data = IOUtils.toByteArray(is);
            b64 = FilenameUtils.getExtension(path) + "|" + Base64.encodeBytes(data);
        } catch (IOException e) {
            // Do nothing
        }
        return b64;
    }

    private void writeTPK(CommandLine cli, TPK tpk) {
        Gson gson = new GsonBuilder().setPrettyPrinting().create();
        String json = gson.toJson(tpk);

        boolean zip = !cli.hasOption(OPTION_NO_ZIP);

        String name = cli.getOptionValue(OPTION_NAME);
        name = name.replace(' ', '_');
        String fn = name + ".json";

        try {
            if (zip) {
                String fnZip = name + ".zip";
                FileOutputStream fos = new FileOutputStream(fnZip);
                ZipOutputStream zos = new ZipOutputStream(fos);
                ZipEntry ze = new ZipEntry(fn);
                zos.putNextEntry(ze);
                zos.write(json.getBytes());
                zos.closeEntry();
                zos.close();
                System.out.println("Wrote TPK to file '" + fnZip + "'");
            } else {
                FileOutputStream fos = new FileOutputStream(fn);
                fos.write(json.getBytes());
                fos.close();
                System.out.println("Wrote TPK to file '" + fn + "'");
            }
        } catch (IOException e) {
            System.err.println("Problem writing TPK.");
        }
    }

    private void run(String[] args) {

        Options options = createCLIOptions();
        CommandLineParser parser = new DefaultParser();
        try {
            CommandLine cli = parser.parse(options, args);
            checkCLI(cli);
            if (cli.hasOption(OPTION_HELP)) {
                showHelp(options);
            }
            TPK tpk = generateTPK(cli);
            writeTPK(cli, tpk);
        } catch (ParseException exp) {
            System.err.println("Parsing failed.  Reason: " + exp.getMessage());
            System.err.println();
            showHelp(options);
        } catch (CLIException e) {
            System.err.println(e.getMessage());
        }
    }

    public static void main(String[] args) {
        new Main().run(args);
    }
}
