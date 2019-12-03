import java.io.*;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.*;
import java.util.stream.Collectors;




public class TestAlgPlus {

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

    /**
     * This method takes care of getting the classes contained in the folder algos and getting the instance of the class
     * represented by those files.
     *
     * @param cryptoParentFolder Parent folder of crypto
     * @return  list of classes of the file contained in the directory /crypto/algos
     * @throws IOException
     */
    public List<Class<?>> getListOfAlgos(File cryptoParentFolder) throws IOException {


        File algoRoot = new File(cryptoParentFolder.getPath()+"/crypto/algos");
        List<String> listOfNames =  Arrays.stream(algoRoot.listFiles())
                .map(x->"crypto.algos." + x.getName().replace(".class", ""))
                .collect(Collectors.toList());

        return listOfNames.stream()
                .map(x-> {
                    try {

                        return getClassFromString(x, cryptoParentFolder);

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

    /**
     *
     * This method takes care of reading the file keys.list line by line and converting each line in a couple of
     * type <Class, Key> that will be inserted in the keyRegistry map and then returned as result.
     *
     * @param cryptoParentFolder Parent folder of crypto
     * @return An object of type KeyRegistry which is a wrapper of an HashMap that contains couples of type <Class, Key>
     */
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


    /**
     *
     * This method takes care of testing the encryption algorithm by instantiating an instance of the one and then
     * invoking the methods of encryption and decryption checking the correctness of the ones
     *
     * @param constructor Costructor of the algorithm to test
     * @param key Encryption key for the algorithm to test
     * @param listOfValidMethods List of methods of the class to test
     * @param secret Secret word that will be used to test the algorithm
     * @throws IllegalAccessException
     * @throws InvocationTargetException
     * @throws InstantiationException
     */
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


    /**
     *
     * This method first reads all the couples <class, key> of the file keys.list invoking the method
     * getKeyRegistryFromFile(Sring), then it occupies to retrieve all the algorithm's classes from the folder "algos" by
     * invoking getListOfAlgos(String). Once it has both the information it starts the process of instantiation of the classes
     * previously mentioned.
     * Once it has retrieved all the necessary data, it call the method  testAlgorithm(...) which will test the algorithm.
     *
     * @param path path of the parent directory of crypto
     * @throws IOException
     */

    public void checkAlgorithms(String path) throws IOException {
        File cryptoParentFolder = new File(path);

        KeyRegistry keyRegistry = getKeyRegistryFromFile(cryptoParentFolder);

        List<Class<?>> listOfAlgorithms = getListOfAlgos(cryptoParentFolder);

        listOfAlgorithms.stream()
                .forEach(x -> {

                    String key = keyRegistry.get(x);
                    Constructor<?> constructor = null;
                    List<Method> listOfValidMethodsByName = new ArrayList<>();
                    try {
                        constructor = x.getDeclaredConstructor(String.class);
                        listOfValidMethodsByName = Arrays.stream(x.getDeclaredMethods())
                                .filter(y -> y.getParameterTypes().length == 1 && y.getParameterTypes()[0] == String.class)
                                .filter(y -> y.getName().startsWith("dec") || y.getName().startsWith("enc"))
                                .collect(Collectors.toList());
                        if(listOfValidMethodsByName.size() == 0)
                            listOfValidMethodsByName =
                                    Arrays.stream(x.getDeclaredMethods())
                                    .filter(y ->Arrays.stream(y.getDeclaredAnnotations())
                                                .filter(z -> z.annotationType().getName().equals("crypto.annot.Encrypt")
                                                        || z.annotationType().getName().equals("crypto.annot.Decrypt"))
                                                .count() >= 1)
                                    .collect(Collectors.toList());


                    } catch (NoSuchMethodException e) {
                        e.printStackTrace();
                    }

                    if(constructor != null  && (listOfValidMethodsByName.size() == 2)){

                        File secret = new File(path +"/crypto/secret.list");

                        Constructor<?> finalConstructor = constructor;
                        List<Method> finalListOfValidMethods = listOfValidMethodsByName;

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

        Scanner sc = new Scanner(System.in);
        System.out.println("Enter the path for the parent folder of 'crypto'");


        new TestAlgPlus().checkAlgorithms(sc.nextLine());
    }

}
