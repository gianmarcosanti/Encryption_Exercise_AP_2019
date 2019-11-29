import javax.management.ObjectName;
import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;




public class TestAlg {

    private List<String> getListOfStringFromFile(File file){

        List<String> listToReturn = new ArrayList<>();

        try (BufferedReader bufferedReader = new BufferedReader(new FileReader(file))) {

            String line = bufferedReader.readLine();

            while(line != null){
                listToReturn.add(line);
                line = bufferedReader.readLine();
            }
        }catch (IOException e) {
            e.printStackTrace();
        }

        return listToReturn;
    }


    public List<Class<?>> getListOfAlgos(File root) throws IOException {


        File algoRoot = new File(root.getPath()+"/crypto/algos");
        List<String> listOfNames =  Arrays.stream(algoRoot.listFiles())
                .map(x->"crypto.algos." + x.getName().replace(".class", ""))
                .collect(Collectors.toList());

        return listOfNames.stream()
                .map(x-> {
                    try {

                        return getClassFromString(x, root);

                    } catch (MalformedURLException | ClassNotFoundException e) {
                        e.printStackTrace();
                    }
                    return null;
                }).collect(Collectors.toList());
    }


    public Class<?> getClassFromString(String c, File cryptoParentFolder) throws MalformedURLException, ClassNotFoundException {

        URL url = cryptoParentFolder.toURI().toURL();
        URL[] urls = new URL[]{url};

        ClassLoader classLoader = new URLClassLoader(urls);

        return classLoader.loadClass(c);
    }

    public KeyRegistry getKeyRegistryFromFile(File cryptoParentFolder){

        File keyList = new File(cryptoParentFolder + "/crypto/keys.list");

        KeyRegistry keyRegistry = new KeyRegistry();


        getListOfStringFromFile(keyList).stream().forEach( x -> {
            String[] items = x.split(" ");
            try {

                keyRegistry.add(getClassFromString(items[0], cryptoParentFolder), items[1]);

            } catch (MalformedURLException | ClassNotFoundException e) {
                e.printStackTrace();
            }
        });

        return keyRegistry;

    }

    public void testAlgorithm(Constructor<?> constructor, String key, List<Method> listOfValidMethods, String secret) throws IllegalAccessException, InvocationTargetException, InstantiationException {

        Object algorithm = constructor.newInstance(key);
        Object e = null;
        Object d = null;


        if(listOfValidMethods.get(0).getName().startsWith("enc")) {
            e = listOfValidMethods.get(0).invoke(algorithm, secret);
            d = listOfValidMethods.get(1).invoke(algorithm, e);

        }else {
            e = listOfValidMethods.get(1).invoke(algorithm, secret);
            d = listOfValidMethods.get(0).invoke(algorithm, e);
        }
        if( !d.toString().replace("#", "").equals(secret))
            System.out.println("KO: " + secret + " -> " + e + " ->" + d);
    }


    public void checkAlgorithms(String path) throws IOException {
        File cryptoParentFolder = new File(path);

        KeyRegistry keyRegistry = getKeyRegistryFromFile(cryptoParentFolder);

        List<Class<?>> listOfAlgorithms = getListOfAlgos(cryptoParentFolder);

        listOfAlgorithms.stream()
                .forEach(x -> {

                    String key = keyRegistry.get(x);
                    Constructor<?> constructor = null;
                    List<Method> listOfValidMethods = null;
                    try {
                        constructor = x.getDeclaredConstructor(String.class);
                        listOfValidMethods = Arrays.stream(x.getDeclaredMethods())
                                .filter(y -> y.getParameterTypes().length == 1 && y.getParameterTypes()[0] == String.class)
                                .filter(y -> y.getName().startsWith("dec") || y.getName().startsWith("enc"))
                                .collect(Collectors.toList());
                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    if(constructor != null && listOfValidMethods != null && listOfValidMethods.size() == 2){

                        File secret = new File(path +"/crypto/secret.list");

                        Constructor<?> finalConstructor = constructor;
                        List<Method> finalListOfValidMethods = listOfValidMethods;

                        getListOfStringFromFile(secret).stream()
                                .forEach(y -> {
                                    try {
                                        testAlgorithm(finalConstructor, key, finalListOfValidMethods, y);
                                    } catch (IllegalAccessException | InvocationTargetException | InstantiationException e) {
                                        e.printStackTrace();
                                    }
                                });

                    }else{
                        System.out.println(x.getName() + " enc/dec not found!");
                    }

                });



    }

    public static void main(String[] argv) throws IOException {

        // /Users/gianmarcosanti/Library/Mobile Documents/com~apple~CloudDocs/UNIPI/AdvancedProgramming/FirstAssigment
        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the path for the parent folder of 'crypto'");


        new TestAlg().checkAlgorithms(sc.nextLine());
    }

}
