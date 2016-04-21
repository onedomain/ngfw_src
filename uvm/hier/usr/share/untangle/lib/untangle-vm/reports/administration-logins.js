{
    "category": "Administration",
    "type": "EVENT_LIST",
    "conditions": [],
    "defaultColumns": ["time_stamp","login","local","client_addr","succeeded","reason"],
    "description": "All local administrator logins.",
    "displayOrder": 1010,
    "javaClass": "com.untangle.node.reports.ReportEntry",
    "table": "admin_logins",
    "conditions": [
        {
            "column": "login",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "!=",
            "value": "localadmin"
        },
        {
            "column": "client_addr",
            "javaClass": "com.untangle.node.reports.SqlCondition",
            "operator": "!=",
            "value": "127.0.0.1"
        }
    ],
    "title": "Admin Logins",
    "uniqueId": "administration-9cVz18dM"
}