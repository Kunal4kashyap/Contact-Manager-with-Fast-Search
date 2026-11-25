import java.io.*;
import java.nio.file.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class Main {
    static class Contact {
        final String id;
        String name, phone, email;
        Contact(String id, String name, String phone, String email) {
            this.id = id; this.name = name; this.phone = phone; this.email = email;
        }
        public String toString() { return id + " | " + name + " | " + phone + (email.isEmpty() ? "" : " | " + email); }
    }
    // An actual binary search tree; contacts with equal names share a node.
    static class NameIndex {
        static class Node {
            String key; List<Contact> matches = new ArrayList<>(); Node left, right;
            Node(String key, Contact c) { this.key = key; matches.add(c); }
        }
        Node root;
        static String key(String s) { return s.toLowerCase(Locale.ROOT); }
        void add(Contact c) { root = insert(root, key(c.name), c); }
        Node insert(Node n, String k, Contact c) {
            if (n == null) return new Node(k,c);
            int cmp = k.compareTo(n.key);
            if (cmp < 0) n.left = insert(n.left,k,c);
            else if (cmp > 0) n.right = insert(n.right,k,c);
            else n.matches.add(c);
            return n;
        }
        List<Contact> find(String name) {
            Node n = root; String k = key(name);
            while (n != null) {
                int cmp = k.compareTo(n.key);
                if (cmp == 0) return new ArrayList<>(n.matches);
                n = cmp < 0 ? n.left : n.right;
            }
            return Collections.emptyList();
        }
        void remove(Contact c) { root = remove(root,key(c.name),c); }
        Node remove(Node n, String k, Contact c) {
            if (n == null) return null;
            int cmp = k.compareTo(n.key);
            if (cmp < 0) n.left = remove(n.left,k,c);
            else if (cmp > 0) n.right = remove(n.right,k,c);
            else { n.matches.remove(c); if (n.matches.isEmpty()) return deleteNode(n); }
            return n;
        }
        Node deleteNode(Node n) {
            if (n.left == null) return n.right;
            if (n.right == null) return n.left;
            Node successor = n.right;
            while (successor.left != null) successor = successor.left;
            n.key = successor.key; n.matches = successor.matches;
            n.right = removeMin(n.right); return n;
        }
        Node removeMin(Node n) { if (n.left == null) return n.right; n.left = removeMin(n.left); return n; }
        List<Contact> ordered() { List<Contact> out = new ArrayList<>(); traverse(root,out); return out; }
        void traverse(Node n, List<Contact> out) { if(n != null) { traverse(n.left,out); out.addAll(n.matches); traverse(n.right,out); } }
    }
    final LinkedList<Contact> contacts = new LinkedList<>();
    final HashMap<String,Contact> phones = new HashMap<>();
    final NameIndex names = new NameIndex();
    int nextId = 101;
    final Path file;
    Main(Path file) { this.file = file; }
    static boolean validText(String s) { return !s.contains("\t") && !s.contains("\n") && !s.contains("\r"); }
    String validate(String name, String phone, String email, Contact existing) {
        if (name.isBlank() || !validText(name) || !validText(email)) return "Invalid name or email.";
        if (!phone.matches("[0-9]{10}")) return "Phone must contain exactly 10 digits.";
        if (phones.containsKey(phone) && phones.get(phone) != existing) return "Duplicate phone number.";
        return null;
    }
    void index(Contact c) { contacts.add(c); phones.put(c.phone,c); names.add(c); }
    String add(String name,String phone,String email) {
        String error = validate(name,phone,email,null); if(error != null) return error;
        Contact c = new Contact("C" + nextId++,name,phone,email); index(c); return "Contact added: " + c;
    }
    String update(String oldPhone,String name,String phone,String email) {
        Contact c = phones.get(oldPhone); if(c == null) return "Contact not found.";
        String error = validate(name,phone,email,c); if(error != null) return error;
        names.remove(c); phones.remove(c.phone);
        c.name=name; c.phone=phone; c.email=email; names.add(c); phones.put(phone,c);
        return "Contact updated: " + c;
    }
    String delete(String phone) {
        Contact c=phones.remove(phone); if(c==null) return "Contact not found.";
        names.remove(c); contacts.remove(c); return "Contact deleted: " + c.id;
    }
    void load() throws IOException {
        if (!Files.exists(file)) return;
        List<String> lines=Files.readAllLines(file,StandardCharsets.UTF_8);
        Set<String> ids=new HashSet<>();
        for(String line:lines) {
            if(line.isBlank()) continue;
            if(line.startsWith("NEXT_ID=")) { try { nextId=Math.max(nextId,Integer.parseInt(line.substring(8))); } catch(NumberFormatException e) { throw new IOException("Invalid ID counter",e); } continue; }
            String[] p=line.split("\t",-1);
            if(p.length!=4 || !p[0].matches("C[0-9]+") || !ids.add(p[0]) || validate(p[1],p[2],p[3],null)!=null) throw new IOException("Invalid or duplicate record in contacts.txt");
            try { nextId=Math.max(nextId,Math.addExact(Integer.parseInt(p[0].substring(1)),1)); } catch(RuntimeException e) { throw new IOException("Invalid contact ID",e); }
            index(new Contact(p[0],p[1],p[2],p[3]));
        }
    }
    void save() throws IOException {
        List<String> lines=new ArrayList<>(); lines.add("NEXT_ID="+nextId);
        for(Contact c:contacts) lines.add(String.join("\t",c.id,c.name,c.phone,c.email));
        Path tmp=file.resolveSibling(file.getFileName()+".tmp");
        Files.write(tmp,lines,StandardCharsets.UTF_8);
        try { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE); }
        catch(AtomicMoveNotSupportedException e) { Files.move(tmp,file,StandardCopyOption.REPLACE_EXISTING); }
    }
    static String read(Scanner sc,String prompt) { System.out.print(prompt); return sc.nextLine().trim(); }
    static void display(Collection<Contact> list) { if(list.isEmpty()) System.out.println("Contact not found."); else for(Contact c:list) System.out.println(c); }
    void menu(Scanner sc) throws IOException {
        while(true) {
            System.out.println("\n1. Add  2. Display All  3. Search  4. Sort  5. Update  6. Delete  7. Exit");
            switch(read(sc,"Choice: ")) {
                case "1": System.out.println(add(read(sc,"Name: "),read(sc,"Phone: "),read(sc,"Email (optional): "))); break;
                case "2": display(contacts); break;
                case "3":
                    String mode=read(sc,"1. Phone  2. Name: ");
                    if(mode.equals("1")) { Contact c=phones.get(read(sc,"Phone: ")); System.out.println(c==null?"Contact not found.":c); }
                    else if(mode.equals("2")) display(names.find(read(sc,"Name: ")));
                    else System.out.println("Invalid choice."); break;
                case "4":
                    String order=read(sc,"1. Ascending  2. Descending  3. BST ordered browse: ");
                    if(order.equals("3")) display(names.ordered());
                    else if(order.equals("1") || order.equals("2")) { List<Contact> sorted=new ArrayList<>(contacts); Comparator<Contact> cmp=Comparator.comparing(c->NameIndex.key(c.name)); sorted.sort(order.equals("2")?cmp.reversed():cmp); display(sorted); }
                    else System.out.println("Invalid choice."); break;
                case "5":
                    String old=read(sc,"Current phone: ");
                    if(!phones.containsKey(old)) System.out.println("Contact not found.");
                    else System.out.println(update(old,read(sc,"New name: "),read(sc,"New phone: "),read(sc,"New email (optional): "))); break;
                case "6": System.out.println(delete(read(sc,"Phone: "))); break;
                case "7": save(); System.out.println("Contacts saved. Goodbye!"); return;
                default: System.out.println("Invalid choice.");
            }
        }
    }
    public static void main(String[] args) {
        Main app=new Main(Paths.get("contacts.txt"));
        try { app.load(); } catch(IOException e) { System.err.println("Cannot load contacts: "+e.getMessage()+". Original file preserved."); return; }
        try(Scanner sc=new Scanner(System.in)) { app.menu(sc); }
        catch(NoSuchElementException e) { try { app.save(); } catch(IOException ex) { System.err.println("Save failed: "+ex.getMessage()); } }
        catch(IOException e) { System.err.println("Save failed: "+e.getMessage()); }
    }
}
