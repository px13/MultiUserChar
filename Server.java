import java.util.*;
import java.io.*;
import java.net.*;
import java.lang.Thread;

public class Server implements Runnable
{
	Map<String, User> users;
	ServerSocket server;
	int port = 1234;
	String sprava = "";
	Queue<Msg> fronta;
	Thread th1;
	Thread th2;
	
	public static void main(String[] args)
	{
		Thread t = new Thread(new Server(), "Server");
		t.start();
	}
	
	public void run()
	{
		users = new HashMap<>();
		try
		{
			server = new ServerSocket(port);
		}
		catch (IOException e)
		{
			e.printStackTrace();
			return;
		}
		fronta = new LinkedList<>();
		
		th1 = new Thread()
		{
			public void run()
			{
				while (true)
				{
					Iterator it = users.entrySet().iterator();
					Set<String> removeUsers = new HashSet<> ();
					while (it.hasNext()) {
						User user = (User) ((Map.Entry) it.next()).getValue();
						if (prijmiSpravu(user, false))
						{
							System.out.println("Prijal som tuto spravu: " + sprava);
							Msg msg = parseSprava(sprava);
							if (msg.pre.equals("Server"))
							{
								removeUsers.add(msg.od);
								List<String> us2 = new ArrayList<String>(users.keySet());
								for (int i = 0 ; i < us2.size() ; i++)
								{
									fronta.add(new Msg("Server:"+msg.od, us2.get(i), msg.od + " sa odhlasil."));
								}
							}
							else
							{
								fronta.add(msg);
							}							
						}	
					}
					users.keySet().removeAll(removeUsers);
					//System.out.println("tik1");
					try {sleep(50);} catch (InterruptedException e) {e.printStackTrace();} // docasne riesenie
				}
			}
		};
		
		th2 = new Thread()
		{
			public void run()
			{
				while (true)
				{
					while (!fronta.isEmpty())
					{
						Msg msg = fronta.remove();
						User user = (User) users.get(msg.pre);
						if (user == null)
						{
							if (!msg.od.equals("Server"))
							{
								fronta.add(new Msg("Server", msg.od, "Sprava pre " + msg.pre + " nemohla byt dorucena."));
							}
						}
						else
						{
							posliSpravu(user, msg.od + ":" + msg.co);
						}
					}
					//System.out.println("tik2");
					try {sleep(50);} catch (InterruptedException e) {e.printStackTrace();} // docasne riesenie
				}
			}
		};
		
		th1.start();
		th2.start();
		
		while (true)
		{
			try
			{
				System.out.println("Cakam na uzivatelov...");
				Socket socket = server.accept();
				User user = new User(socket, "");
				prijmiSpravu(user, true);
				Msg msg = parseSprava(sprava);
				user.name = msg.od;
				String zoznamUzivatelov = "";
				List<String> us = new ArrayList<String>(users.keySet());
				for (int i = 0 ; i < us.size() ; i++)
				{
					fronta.add(new Msg("Server:"+user.name, us.get(i), user.name + " sa prihlasil."));
					zoznamUzivatelov += us.get(i) + ";";
				}
				fronta.add(new Msg("Server", user.name, zoznamUzivatelov));
				users.put(user.name, user);
				System.out.println(user.name + " sa prihlasil.");
			}
			catch (IOException e)
			{
				break;
			}
		}
	}
	
	public Msg parseSprava(String sp)
	{
		Msg msg = new Msg();
		String buff = "";
		int i = 0;
		while (sp.charAt(i) != ':')
		{
			buff += sp.charAt(i);
			i++;
		}
		i++;
		msg.od = buff;
		buff = "";
		while (sp.charAt(i) != ':')
		{
			buff += sp.charAt(i);
			i++;
		}
		i++;
		msg.pre = buff;
		buff = "";
		while (i != sp.length())
		{
			buff += sp.charAt(i);
			i++;
		}
		msg.co = buff;
		buff = "";
		return msg;
	}

	public void posliSpravu(User u, String message)
	{
		byte bts[] = message.getBytes();
		    try {
			    u.wr.write(bts.length & 255);
			    u.wr.write(bts.length >> 8);
			    u.wr.write(bts, 0, bts.length);
			    u.wr.flush();
			} catch (IOException e) {
				e.printStackTrace();
			}
	}
	
	public boolean prijmiSpravu(User u, boolean blocking)
	{
		try {
			if (!blocking && u.rd.available() == 0) return false;
			int nbts = u.rd.read() + (u.rd.read() << 8);
			byte bts[] = new byte[nbts];
			int i = 0; // how many bytes did we read so far
			do {
				int j = u.rd.read(bts, i, bts.length - i);
				if (j > 0) i += j;
				else break;
			} while (i < bts.length);
			sprava = new String(bts);
		} catch (IOException e) {
			return false;
		}
		return true;
	}
}

class User
{
	public OutputStream wr; 
	public InputStream rd;
	public Socket socket;
	public String name;
	
	public User(Socket s, String n) throws IOException
	{
		socket = s;
		name = n;
		wr = socket.getOutputStream();
        rd = socket.getInputStream();
	}
}

class Msg
{
	public String od;
	public String pre;
	public String co;
	
	public Msg()
	{
		od = "";
		pre = "";
		co = "";
	}
	
	public Msg(String nOd, String nPre, String nCo)
	{
		od = nOd;
		pre = nPre;
		co = nCo;
	}
}

