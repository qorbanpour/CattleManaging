package damdariar.gui.swing.search;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Vector;

import javax.swing.JButton;
import javax.swing.JComponent;

import org.hibernate.Query;
import org.hibernate.Session;
import org.hibernate.metadata.ClassMetadata;

import damdariar.gui.editor.DMultiDisplayCombo;
import damdariar.gui.editor.EditorI;
import damdariar.gui.swing.DButton;
import damdariar.gui.swing.DComboBox;
import damdariar.gui.swing.DPanel;
import damdariar.gui.swing.forms.FormUtility;
import damdariar.gui.swing.forms.editorvisibility.EditorVisibility;
import damdariar.gui.swing.layout.GridLayoutManager;
import damdariar.hibernate.DNamingStrategy;
import damdariar.hibernate.HibernateUtil;
import damdariar.images.ImageUtil;
import damdariar.resource.ResourceUtil;

public class AdvancedSearchPanel extends DPanel {

	/**
	 * 
	 */
	List<DComboBox>  columnComboBoxes=new ArrayList<DComboBox>();
	List<DComboBox>  operatorsComboBoxes = new ArrayList<DComboBox>();
	List<DComboBox>  conditionsComboBoxes = new ArrayList<DComboBox>();
	
	List<Object>     values=new ArrayList<Object>();
	List<ConditionSearchEntryPair>     searchEntries=new ArrayList<ConditionSearchEntryPair>();
	Map<DComboBox,JComponent> editors  = new HashMap<DComboBox ,JComponent >();
	
	DButton      add;
	DButton      remove;
	String       addButtonText = " \u0627\u0641\u0632\u0648\u062f\u0646 \u0634\u0631\u0637 \u062c\u062f\u06cc\u062f";
	String       removeButtonText = " \u062d\u0630\u0641 \u0634\u0631\u0637 \u0627\u0636\u0627\u0641\u0647 \u0634\u062f\u0647";
	
	String       andOperator = " \u0648 ";
	String       orOperator  = "  \u06cc\u0627 ";
	
	
	final static String       fieldName_OpenParanthesis      = "  ) ";
	final static String       fieldName_CloseParanathesis    = "  ( ";
	
	
	String      greater = " \u0628\u0632\u0631\u06af\u062a\u0631";
	String      lower   = " \u06a9\u0648\u0686\u06a9\u062a\u0631";
	String      equal   = " \u0645\u0633\u0627\u0648\u06cc";
	String      lowerEqual = " \u06a9\u0648\u0686\u06a9\u062a\u0631 \u0645\u0633\u0627\u0648\u06cc";
	String      greaterEqual = " \u0628\u0632\u0631\u06af\u062a\u0631 \u0645\u0633\u0627\u0648\u06cc";
	String      notEqual = " \u0646\u0627 \u0645\u0633\u0627\u0648\u06cc";
	
	String[]     operators = new String[]{equal,notEqual,greater,greaterEqual,lower,lowerEqual};
	String[]     conditions = new String[]{andOperator,orOperator};
	
	
	
	
	DButton     disabledButton;
	Vector<PropertyNameTranslationPair>    fieldNames ;
	ClassMetadata metaData;
	EditorVisibility visibilotyClass;
	String           propertyNames[];
	
	static String      isRangeActionCommad = "ISRANGEACTIONCOMMAND";
	AdvancedSearchPanelLayoutManager        layoutManager;
	Class<?> classType;
	private String linkedProperty;
	
	public AdvancedSearchPanel(Class<?> classType, String linkedProperty){
		this.classType = classType;
		this.linkedProperty = linkedProperty;
		layoutManager = new AdvancedSearchPanelLayoutManager(this);
		
		visibilotyClass = FormUtility.getEditorVisibilityClass(classType);
		metaData = HibernateUtil.getMetaData(classType);
		
		fieldNames = new Vector<PropertyNameTranslationPair>();
		String propertyNames[] = metaData.getPropertyNames();
		
		for (int i = 0; i < propertyNames.length; i++) {
			if(!visibilotyClass.isVisible(propertyNames[i]) || propertyNames[i].equalsIgnoreCase(this.linkedProperty))
				continue;
			String propertyTranslation = ResourceUtil.getPropertyTranslation(classType.getSimpleName(), propertyNames[i]);
			fieldNames.add(new PropertyNameTranslationPair(propertyTranslation,propertyNames[i]));
		}
		
		fieldNames.add(new PropertyNameTranslationPair(fieldName_OpenParanthesis,fieldName_OpenParanthesis));
		fieldNames.add(new PropertyNameTranslationPair(fieldName_CloseParanathesis,fieldName_CloseParanathesis));

		addNewSearchCriteria(null);
		
	}
	
	public static  boolean isParanthesis(String columnName){
		if(columnName.equalsIgnoreCase(fieldName_OpenParanthesis) || columnName.equalsIgnoreCase(fieldName_CloseParanathesis))
			return true;
		return false;
	}
	public Query getSearchCriteria(){
		DNamingStrategy namingStrategy =  new DNamingStrategy();
		String tableName =namingStrategy.classToTableName(classType.getSimpleName());
		StringBuffer   buff = new StringBuffer( " WHERE ");
		StringBuffer   joinClause = new StringBuffer("select *  from "+tableName+"  "+tableName );
		List<Object>  values = new ArrayList<Object>();
    	for(int i = 0 ; i < columnComboBoxes.size() ; i++){
    		
    		 DComboBox  column =  columnComboBoxes.get(i);
    		 String columnName =namingStrategy.propertyToColumnName(((PropertyNameTranslationPair)column.getSelectedItem()).getPropertyName());
    		 ConditionSearchEntryPair searchConditionEntries = searchEntries.get(i);
    		 
    		 if(isParanthesis(columnName)){
    			 getConditionString(buff,searchConditionEntries);
    			 if(columnName.equalsIgnoreCase(fieldName_OpenParanthesis))
    				 buff.append(fieldName_CloseParanathesis);
    			 else
    				 buff.append(fieldName_OpenParanthesis);
    			 continue;
    		 }	 
    		
    		 EditorI   editor = (EditorI) editors.get(column);
    		 Object    editorValue = (editor instanceof DMultiDisplayCombo) ?  ((DMultiDisplayCombo)editor).getForeignValue() : editor.getValue();
    		 if(editorValue == null)
    			 continue;
    		 values.add(editorValue);
    		 getConditionString(buff,searchConditionEntries);
    		 if(editor instanceof DMultiDisplayCombo){
    			 joinClause.append(((DMultiDisplayCombo)editor).getJoinClause());
    			 buff.append(((DMultiDisplayCombo)editor).getColumnName());
    		 }
    		 else
    			 buff.append(tableName).append(".").append(columnName);
    		 DComboBox operatorComboBox = operatorsComboBoxes.get(i);
    		 String operator =   (String) operatorComboBox.getSelectedItem();
    		 
    		 
    		 buff.append(getEditorSQL(editor,operator));
    		 
    	}
    	String identifierName = HibernateUtil.getMetaData(classType).getIdentifierPropertyName();
    	buff.append( " order by "+tableName+"."+namingStrategy.propertyToColumnName(identifierName));
    	Session session = HibernateUtil.getSession();
    	Query query =  session.createSQLQuery(
   		        joinClause.toString()+buff.toString()).addEntity(classType);
    	int valueCounter = 0;
    	for(Object value:values){
    		if(value instanceof String)
    			query.setString(valueCounter, (String) value);
    		else
    		if(value instanceof Integer)
    			query.setInteger(valueCounter, (Integer)value);
    		else
        	if(value instanceof Double)
        		query.setDouble(valueCounter, (Double)value);
        	else
            if(value instanceof Boolean)
            	query.setBoolean(valueCounter, (Boolean)value);
            else
            if(value instanceof Timestamp)
               query.setTimestamp(valueCounter, (Timestamp)value);
            else
            if(value instanceof Date)
               query.setDate(valueCounter, (Date)value);
    		
    		valueCounter++;
    		
    	}
    	
    	return query;
	}
	
	private void getConditionString(StringBuffer buff, ConditionSearchEntryPair searchConditionEntries){
		
		 if(searchConditionEntries.getConditionCombo() != null && searchConditionEntries.getConditionCombo().isEnabled()){
			 String condition = (String) searchConditionEntries.getConditionCombo().getSelectedItem();
			 if(condition.equalsIgnoreCase(orOperator))
				    buff.append(" OR ");
			 else
				   buff.append(" AND ");
		 }
	}
	
	
	private String getEditorSQL(EditorI editor, String operator) {
		String sqlOperator = "=";

		String      greater = " \u0628\u0632\u0631\u06af\u062a\u0631";
		String      lower   = " \u06a9\u0648\u0686\u06a9\u062a\u0631";
		String      equal   = " \u0645\u0633\u0627\u0648\u06cc";
		String      lowerEqual = " \u06a9\u0648\u0686\u06a9\u062a\u0631 \u0645\u0633\u0627\u0648\u06cc";
		String      greaterEqual = " \u0628\u0632\u0631\u06af\u062a\u0631 \u0645\u0633\u0627\u0648\u06cc";
		String      notEqual = " \u0646\u0627 \u0645\u0633\u0627\u0648\u06cc";
		
		if(operator.equalsIgnoreCase(greater))
			sqlOperator = ">";
		else 
		if(operator.equalsIgnoreCase(lower))
			sqlOperator = "<";
		else 
		if(operator.equalsIgnoreCase(equal))
				sqlOperator = "=";
		else 
		if(operator.equalsIgnoreCase(notEqual))
					sqlOperator = "!=";
		else 
		if(operator.equalsIgnoreCase(greaterEqual))
						sqlOperator = ">=";
		if(operator.equalsIgnoreCase(lowerEqual))
			sqlOperator = "<=";
		
		return	" "+ sqlOperator+ " ? "; 
	
	}

	public void addNewSearchCriteria(DComboBox conditionComboBox){
		
		
	    DPanel     searchEntry = new DPanel();
		SearchEntryLayoutManager  searchEntryLayout = new SearchEntryLayoutManager(searchEntry);
			
		DComboBox  columnComb = new DComboBox(fieldNames);
		columnComboBoxes.add(columnComb);
		
		
		DComboBox  operatorCombo = new DComboBox(operators);
		operatorsComboBoxes.add(operatorCombo);
		
		searchEntryLayout.add(columnComb);
		searchEntryLayout.add(operatorCombo);
		
		DPanel    editorPanel = new DPanel(new BorderLayout());
		JComponent editor = (JComponent) FormUtility.getEditor(classType,metaData,fieldNames.get(0).getPropertyName());
		editors.put(columnComb,editor);
		editorPanel.setPreferredSize(new Dimension(100,30));
		editorPanel.add(editor);
		searchEntryLayout.add(editorPanel);
	
		remove = new DButton(ImageUtil.getImageIcon("delete.gif"));
	

		add = new DButton(ImageUtil.getImageIcon("add_16.png"));
		add.addActionListener(new AddActionListener(this,columnComb));

		columnComb.addActionListener(new FieldComboAction(this,columnComb,operatorCombo,editor,editorPanel,conditionComboBox));
		
		searchEntryLayout.add(add);
		searchEntryLayout.add(remove);
		
		ConditionSearchEntryPair conditionSearchEntry= new ConditionSearchEntryPair(searchEntry,conditionComboBox);
		searchEntries.add(conditionSearchEntry);
		remove.addActionListener(new RemoveActionListener(this,conditionSearchEntry,columnComb,operatorCombo));
		
		layoutManager.add(searchEntry);
		
		
	}
	
	
	private static final long serialVersionUID = 1L;
	

	


	
	

}

class SearchButtonPanel extends DPanel{
	
	/**
	 * 
	 */
	private static final long serialVersionUID = 1L;
	GridLayoutManager layout = new GridLayoutManager(this);
	SearchButtonPanel(){
		
		layout.getConstriant().gridx = 3;
		layout.getConstriant().gridy = 2;
		layout.add(searchButton,3,2);
	}
	DButton      searchButton = new DButton(ResourceUtil.getString("ADVANCESEARCH_BUTTON"));
	
	
	
}  

class RemoveActionListener implements ActionListener{
	AdvancedSearchPanel container;
	private ConditionSearchEntryPair searchConditionEntry;
	private DComboBox columnComb;
	private DComboBox operatorCombo;

	
	RemoveActionListener(AdvancedSearchPanel container,ConditionSearchEntryPair searchConditionEntry, DComboBox columnComb, DComboBox operatorCombo){
		this.container = container;
		this.searchConditionEntry = searchConditionEntry;
		this.columnComb = columnComb;
		this.operatorCombo=operatorCombo;

		
	}

	public void actionPerformed(ActionEvent e) {
		int index = -1;
		if((container).searchEntries.size() > 1){
			index = container.columnComboBoxes.indexOf(columnComb);
			container.columnComboBoxes.remove(columnComb);
			container.editors.remove(columnComb);
			container.operatorsComboBoxes.remove(operatorCombo);
		    container.remove(searchConditionEntry.getSearchEntryPanel());
		    if(searchConditionEntry.getConditionCombo() != null)
		    	container.remove(searchConditionEntry.getConditionCombo());
		    ((AdvancedSearchPanel)container).searchEntries.remove(searchConditionEntry);
		   ((AdvancedSearchPanel)container).revalidate();
		   
		}
		else {
			((JButton)e.getSource()).setEnabled(false);
			((AdvancedSearchPanel)container).disabledButton = (DButton) e.getSource();
			
		}
		if(index == 0 && container.searchEntries.size() >= 1){
			container.remove(container.searchEntries.get(0).getConditionCombo());
			container.searchEntries.get(0).setConditionCombo(null);
			container.revalidate();
		}
	}
	
}

class AddActionListener implements ActionListener{
	AdvancedSearchPanel searchPanel;
	private DComboBox   columnCombo;
	
	AddActionListener(AdvancedSearchPanel searchPanel, DComboBox columnComb){
		this.searchPanel = searchPanel;
		this.columnCombo = columnComb;
		
	}

	public void actionPerformed(ActionEvent e) {
		if(searchPanel.disabledButton != null)    
			   	searchPanel.disabledButton.setEnabled(true);
	
		PropertyNameTranslationPair nameTrPair = (PropertyNameTranslationPair) columnCombo.getSelectedItem();
		
		String propertyName = nameTrPair.getPropertyName();
		if(!propertyName.equalsIgnoreCase(AdvancedSearchPanel.fieldName_OpenParanthesis ))
		{
			int oldFill = searchPanel.layoutManager.getConstriant().fill;
			searchPanel.layoutManager.getConstriant().fill = GridBagConstraints.NONE;
			DComboBox  conditionCombo = new DComboBox(searchPanel.conditions);
			((AdvancedSearchPanelLayoutManager)searchPanel.getLayout()).gotoNewLine(); 
			searchPanel.layoutManager.add(conditionCombo);
			searchPanel.layoutManager.getConstriant().fill = oldFill;
			((AdvancedSearchPanelLayoutManager)searchPanel.getLayout()).gotoNewLine(); 
			searchPanel.addNewSearchCriteria(conditionCombo);
			
		}
		else{
			((AdvancedSearchPanelLayoutManager)searchPanel.getLayout()).gotoNewLine(); 
			searchPanel.addNewSearchCriteria(null);
		}
			searchPanel.revalidate();
			searchPanel.disabledButton =  null;
	}
	
}


class FieldComboAction implements ActionListener{

	private AdvancedSearchPanel advancedSearchPanel;
	private DComboBox columnComb;
	private JComponent editor;
	private DComboBox operatorCombo;
	private DPanel editorPanel;
	private DComboBox conditionComboBox;

	public FieldComboAction(AdvancedSearchPanel advancedSearchPanel,
			DComboBox columnComb, DComboBox operatorCombo, JComponent editor,DPanel editorPanel,DComboBox conditionComboBox) {
		this.advancedSearchPanel = advancedSearchPanel;
		this.columnComb = columnComb;
		this.operatorCombo = operatorCombo;
		this.editor = editor;
		this.editorPanel = editorPanel;
		this.conditionComboBox =conditionComboBox; 
	}


	@Override
	public void actionPerformed(ActionEvent e) {
		PropertyNameTranslationPair nameTrPair = (PropertyNameTranslationPair) columnComb.getSelectedItem();
		String propertyName = nameTrPair.getPropertyName();
		if(conditionComboBox != null)
			conditionComboBox.setEnabled(true);
		if(propertyName.equalsIgnoreCase(AdvancedSearchPanel.fieldName_OpenParanthesis ) || 
				propertyName.equalsIgnoreCase(AdvancedSearchPanel.fieldName_CloseParanathesis)){
			operatorCombo.setEnabled(false);
			editor.setEnabled(false);
			if(propertyName.equalsIgnoreCase(AdvancedSearchPanel.fieldName_CloseParanathesis)){
				if(conditionComboBox != null)
					conditionComboBox.setEnabled(false);
			}
		}
		else{
			
			editor = (JComponent) FormUtility.getEditor(advancedSearchPanel.classType, advancedSearchPanel.metaData, propertyName);
			advancedSearchPanel.editors.put(columnComb,editor);
			editorPanel.removeAll();
			editorPanel.add(editor);
			editorPanel.revalidate();
			operatorCombo.setEnabled(true);
			editor.setEnabled(true);
		}
		
	}
	
	
	
	
}
class PropertyNameTranslationPair{
	
	
	String propertyTranslation;
	String propertyName;
	public PropertyNameTranslationPair(String propertyTranslation,
			String propertyName) {
		super();
		this.propertyTranslation = propertyTranslation;
		this.propertyName = propertyName;
	}
	public String getPropertyTranslation() {
		return propertyTranslation;
	}
	public void setPropertyTranslation(String propertyTranslation) {
		this.propertyTranslation = propertyTranslation;
	}
	public String getPropertyName() {
		return propertyName;
	}
	public void setPropertyName(String propertyName) {
		this.propertyName = propertyName;
	}
	
	public String toString(){
		return propertyTranslation;
	}
	
	
	
}

class ConditionSearchEntryPair{
	
	DPanel searchEntryPanel;
	DComboBox conditionCombo;
	public ConditionSearchEntryPair(DPanel searchEntryPanel,
			DComboBox conditionCombo) {
		super();
		this.searchEntryPanel = searchEntryPanel;
		this.conditionCombo = conditionCombo;
	}
	public DPanel getSearchEntryPanel() {
		return searchEntryPanel;
	}
	public void setSearchEntryPanel(DPanel searchEntryPanel) {
		this.searchEntryPanel = searchEntryPanel;
	}
	public DComboBox getConditionCombo() {
		return conditionCombo;
	}
	public void setConditionCombo(DComboBox conditionCombo) {
		this.conditionCombo = conditionCombo;
	}
	
	
}
