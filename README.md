## **Kin-Scheduler**
简单的分布式任务调度平台

### **实现细节**
日志路径:
* basePath/{yyyy-MM-dd}/{workerId}.log
* basePath/{yyyy-MM-dd}/{workerId}/{executorId}.log
* basePath/{yyyy-MM-dd}/{masterId}.log
* basePath/{yyyy-MM-dd}/jobs/{jobId}/{taskId}/{logFileName, 默认=taskId}.log

task output路径:
* basePath/{yyyy-MM-dd}/jobs/{jobId}/{taskId}/{logFileName, 默认=taskId}.out