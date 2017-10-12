package org.aksw.iguana.wc.exec;

import java.io.Serializable;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.SessionScoped;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ViewScoped;
import javax.faces.context.FacesContext;
import javax.inject.Named;

import org.aksw.iguana.commons.config.Config;
import org.aksw.iguana.commons.constants.COMMON;
import org.aksw.iguana.commons.factory.TypedFactory;
import org.aksw.iguana.commons.rabbit.RabbitMQUtils;
import org.aksw.iguana.commons.sender.ISender;
import org.aksw.iguana.commons.sender.impl.DefaultSender;
import org.aksw.iguana.wc.config.ConfigConverter;
import org.aksw.iguana.wc.config.Connection;
import org.aksw.iguana.wc.config.Dataset;
import org.aksw.iguana.wc.config.Stresstest;
import org.aksw.iguana.wc.config.Task;
import org.apache.commons.configuration.Configuration;
import org.primefaces.event.FlowEvent;

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

	@PostConstruct
	public void init() {
		Configuration conf = Config.getInstance();
		this.sender.init(conf.getString(COMMON.CONSUMER_HOST_KEY), COMMON.WC2MC_QUEUE);
	}

	public void initTasks() {
	}

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

	public void addConnection() {
		this.connections.add(createConnection);
		this.createConnection = new Connection();
	}

	public void addDataset() {
		this.datasets.add(createDataset);
		this.createDataset = new Dataset();
	}

	public void addTask() {
		this.tasks.add(createTask);
		this.createTask = new Stresstest();
	}

	public void removeConnection() {
		for (Connection con : connections) {
			if (con.getName().equals(deleteConnection)) {
				this.connections.remove(con);
				break;
			}
		}
		deleteConnection = null;
	}

	public void removeDataset() {
		for (Dataset con : datasets) {
			if (con.getName().equals(deleteDataset)) {
				this.datasets.remove(con);
				break;
			}
		}
		deleteDataset = null;
	}

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

	public String getDeleteConnection() {
		return deleteConnection;
	}

	public void setDeleteConnection(String deleteConnection) {
		this.deleteConnection = deleteConnection;
	}

	public List<Dataset> getDatasets() {
		return datasets;
	}

	public void setDatasets(List<Dataset> datasets) {
		this.datasets = datasets;
	}

	public String getDeleteDataset() {
		return deleteDataset;
	}

	public void setDeleteDataset(String deleteDataset) {
		this.deleteDataset = deleteDataset;
	}

	public Dataset getCreateDataset() {
		return createDataset;
	}

	public void setCreateDataset(Dataset createDataset) {
		this.createDataset = createDataset;
	}

	public void execute() {
		Configuration conf = ConfigConverter.createIguanConfig(connections, datasets, tasks);
		sender.send(RabbitMQUtils.getData(conf));

		FacesContext context = FacesContext.getCurrentInstance();

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

	public void initTask() {
		if(className.equals("org.aksw.iguana.tp.tasks.impl.stresstest.Stresstest")) {
			createTask = new Stresstest();
		}
	}

}
