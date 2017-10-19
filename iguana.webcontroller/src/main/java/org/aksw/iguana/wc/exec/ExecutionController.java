package org.aksw.iguana.wc.exec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;
import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.faces.application.FacesMessage;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.aksw.iguana.wc.config.ConfigConverter;
import org.aksw.iguana.wc.config.Connection;
import org.aksw.iguana.wc.config.Dataset;
import org.aksw.iguana.wc.config.tasks.Stresstest;
import org.aksw.iguana.wc.config.tasks.Task;
import org.apache.commons.configuration.Configuration;
import org.apache.commons.configuration.ConfigurationException;
import org.aksw.iguana.commons.config.ConfigurationUtils;
import org.apache.commons.configuration.PropertiesConfiguration;
import org.primefaces.event.FlowEvent;
import org.primefaces.model.DefaultStreamedContent;
import org.primefaces.model.StreamedContent;

/**
 * Controller to add a Config convert the Configuration to a Properties Object
 * and send it to the Core Controller. Thus starting an Iguana Suite
 * 
 * @author f.conrads
 *
 */
@Named
@ApplicationScoped
public class ExecutionController implements Serializable {

	/**
	 * 
	 */
	private static final long serialVersionUID = 5112905497758755682L;

	private List<Connection> connections = new LinkedList<Connection>();
	private String deleteConnection;
	private Connection createConnection = new Connection();

	private List<Dataset> datasets = new LinkedList<Dataset>();
	private String deleteDataset;
	private Dataset createDataset = new Dataset();

	private List<Task> tasks = new LinkedList<Task>();
	private Integer deleteTask;
	private Stresstest createTask = new Stresstest();

	private String className;

	private ISender sender = new DefaultSender();

	/**
	 * get config and sets the name for the rabbitmq host and the correct queue
	 */
	@PostConstruct
	public void init() {
		Configuration conf = Config.getInstance();
		this.sender.init(conf.getString(COMMON.CONSUMER_HOST_KEY), COMMON.CONFIG2MC_QUEUE_NAME);
	}


	/**
	 * validates the next and previous step
	 * @param event
	 * @return
	 */
	public String onFlowProcess(FlowEvent event) {
		if (this.connections.isEmpty() && event.getOldStep().equals("connections")) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR",
					"No Connections. Please add at least one connection");
			FacesContext.getCurrentInstance().addMessage(null, msg);
			return event.getOldStep();
		} else if (this.datasets.isEmpty() && event.getOldStep().equals("datasets")
				&& !event.getNewStep().equals("connections")) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR",
					"No Datasets. Please add at least one dataset");
			FacesContext.getCurrentInstance().addMessage(null, msg);
			return event.getOldStep();
		} else if (this.tasks.isEmpty() && event.getOldStep().equals("tasks")
				&& !event.getNewStep().equals("datasets")) {
			FacesMessage msg = new FacesMessage(FacesMessage.SEVERITY_ERROR, "ERROR",
					"No Tasks. Please add at least one task");
			FacesContext.getCurrentInstance().addMessage(null, msg);
			return event.getOldStep();
		}
		return event.getNewStep();

	}

	/**
	 * Add the created Connection
	 */
	public void addConnection() {
		this.connections.add(createConnection);
		this.createConnection = new Connection();
	}

	/**
	 * Add the created Dataset
	 */
	public void addDataset() {
		this.datasets.add(createDataset);
		this.createDataset = new Dataset();
	}

	/**
	 * Add the created Task
	 */
	public void addTask() {
		this.tasks.add(createTask);
		this.createTask = new Stresstest();
	}

	/**
	 * Remove selected Connection
	 */
	public void removeConnection() {
		for (Connection con : connections) {
			if (con.getName().equals(deleteConnection)) {
				this.connections.remove(con);
				break;
			}
		}
		deleteConnection = null;
	}

	/**
	 * Remove selected Dataset
	 */
	public void removeDataset() {
		for (Dataset con : datasets) {
			if (con.getName().equals(deleteDataset)) {
				this.datasets.remove(con);
				break;
			}
		}
		deleteDataset = null;
	}

	/**
	 * Remove selected Task
	 */
	public void removeTask() {
		for (Task task : tasks) {
			if (task.hashCode() == deleteTask) {
				this.tasks.remove(task);
				break;
			}
		}
		deleteTask = null;
	}

	/**
	 * @return the connections
	 */
	public List<Connection> getConnections() {
		return connections;
	}

	/**
	 * @param connections
	 *            the connections to set
	 */
	public void setConnections(List<Connection> connections) {
		this.connections = connections;
	}

	/**
	 * @return the createConnection
	 */
	public Connection getCreateConnection() {
		return createConnection;
	}

	/**
	 * @param createConnection
	 *            the createConnection to set
	 */
	public void setCreateConnection(Connection createConnection) {
		this.createConnection = createConnection;
	}

	/**
	 * @return
	 */
	public String getDeleteConnection() {
		return deleteConnection;
	}

	/**
	 * @param deleteConnection
	 */
	public void setDeleteConnection(String deleteConnection) {
		this.deleteConnection = deleteConnection;
	}

	/**
	 * @return
	 */
	public List<Dataset> getDatasets() {
		return datasets;
	}

	/**
	 * @param datasets
	 */
	public void setDatasets(List<Dataset> datasets) {
		this.datasets = datasets;
	}

	/**
	 * @return
	 */
	public String getDeleteDataset() {
		return deleteDataset;
	}

	/**
	 * @param deleteDataset
	 */
	public void setDeleteDataset(String deleteDataset) {
		this.deleteDataset = deleteDataset;
	}

	/**
	 * @return
	 */
	public Dataset getCreateDataset() {
		return createDataset;
	}

	/**
	 * @param createDataset
	 */
	public void setCreateDataset(Dataset createDataset) {
		this.createDataset = createDataset;
	}

	/**
	 * Send the generated config to the correct rabbitmq queue
	 */
	public void execute() {
		FacesContext context = FacesContext.getCurrentInstance();
		Configuration conf = ConfigConverter.createIguanConfig(connections, datasets, tasks);
		try {
			sender.send(RabbitMQUtils.getData(ConfigurationUtils.convertConfiguration(conf)));
		} catch (ConfigurationException e) {
			context.addMessage(null,
					new FacesMessage("Error", "Configuration could not send to Core."));

		}

		

		context.addMessage(null,
				new FacesMessage("Successful", "Configuration was send to Core and will be executed."));
	}

	/**
	 * @return the tasks
	 */
	public List<Task> getTasks() {
		return tasks;
	}

	/**
	 * @param tasks
	 *            the tasks to set
	 */
	public void setTasks(List<Task> tasks) {
		this.tasks = tasks;
	}

	/**
	 * @return the deleteTask
	 */
	public Integer getDeleteTask() {
		return deleteTask;
	}

	/**
	 * @param deleteTask
	 *            the deleteTask to set
	 */
	public void setDeleteTask(Integer deleteTask) {
		this.deleteTask = deleteTask;
	}

	/**
	 * @return the createTask
	 */
	public Task getCreateTask() {
		return createTask;
	}

	/**
	 * @param createTask
	 *            the createTask to set
	 */
	public void setCreateTask(Stresstest createTask) {
		this.createTask = createTask;
	}

	/**
	 * @return the className
	 */
	public String getClassName() {
		return className;
	}

	/**
	 * @param className
	 *            the className to set
	 */
	public void setClassName(String className) {
		this.className = className;
	}

	/**
	 * check if className is of Stresstest and sets the createTask to a new Stresstest object
	 */
	public void initTask() {
		if("org.aksw.iguana.tp.tasks.impl.stresstest.Stresstest".equals(className)) {
			createTask = new Stresstest();
		}
	}
	
	/**
	 * Will save the config as file
	 * 
	 * @return
	 * @throws ConfigurationException
	 */
	public StreamedContent save() throws ConfigurationException {
		Configuration conf = ConfigConverter.createIguanConfig(connections, datasets, tasks);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PropertiesConfiguration saveConf = new PropertiesConfiguration();
		saveConf.copy(conf);
		saveConf.save(baos);
		byte[] data = baos.toByteArray();
		InputStream stream = new ByteArrayInputStream(data);
		return new DefaultStreamedContent(stream, "text/plain", "iguana.suite");
	}

}
