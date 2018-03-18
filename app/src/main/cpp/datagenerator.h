#ifndef SPEEDOMETER_DATATHREAD_H
#define SPEEDOMETER_DATATHREAD_H

#include <jni.h>
#include <chrono>
#include <thread>
#include <functional>

//#include "jlistener.h"

namespace engine {

    class DataGenerator {
        JavaVM *const m_jvm;
        //JListener m_observer;
        std::function<void(JNIEnv*, double)> m_observer;
        volatile bool m_stop;
        std::shared_ptr<std::thread> m_pThread;
        const std::chrono::microseconds m_period;
        std::chrono::microseconds m_workTime;
        std::chrono::system_clock::time_point m_lastTime;

        void run();

    protected:
        virtual double generate(const std::chrono::microseconds &time) = 0;

    public:
        DataGenerator(JavaVM *jvm, std::function<void(JNIEnv*, double)>&& observer, const std::chrono::microseconds &period)
                : m_jvm(jvm),m_observer(observer), m_stop(0), m_period(period), m_workTime(m_period.zero()) {}

        virtual ~DataGenerator();

        void start();

        void stop();

    };

    class SpeedGenerator : public DataGenerator {

    protected:
        double generate(const std::chrono::microseconds &time);

    public:
        SpeedGenerator(JavaVM *jvm, std::function<void(JNIEnv*, double)>&& observer)
                : DataGenerator(jvm, std::move(observer), std::chrono::milliseconds(10)) {}

        virtual ~SpeedGenerator() {}

    };

    class TachoGenerator : public DataGenerator {

    protected:
        double generate(const std::chrono::microseconds &time);

    public:
        TachoGenerator(JavaVM *jvm, std::function<void(JNIEnv*, double)> &&observer)
                : DataGenerator(jvm, std::move(observer), std::chrono::milliseconds(500)) {}

        virtual ~TachoGenerator() {}

    };

}
#endif //SPEEDOMETER_DATATHREAD_H
