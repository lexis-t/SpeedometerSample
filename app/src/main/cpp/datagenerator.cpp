#include <numeric>
#include <math.h>
#include "datagenerator.h"

using namespace engine;

void DataGenerator::run()
{
    JNIEnv *env;
    m_jvm->AttachCurrentThread(&env, NULL);

    for(;;)
    {
        if(m_stop) break;

        std::chrono::system_clock::time_point now = std::chrono::system_clock::now();
        m_workTime += m_lastTime - now;
        m_lastTime = now;

        m_observer(env, generate(m_workTime));

        std::this_thread::sleep_until(m_lastTime + m_period);
    }

    m_jvm->DetachCurrentThread();
}


DataGenerator::~DataGenerator()
{
    stop();
}

void DataGenerator::start()
{
    m_pThread.reset(new std::thread(&DataGenerator::run, this));
}

void DataGenerator::stop()
{
    m_stop = true;
    m_pThread->join();
    m_pThread.reset();
}

// ----------------------------------------------------------------------------------------------------------------------------
// class SpeedGenerator

double SpeedGenerator::generate(const std::chrono::microseconds& time)
{
    double_t seconds = static_cast<double_t>(time.count()) / 1000000.0;

    double_t res = 100.0 * (sin(seconds) + 1.0) + 30.0 * (sin(seconds * 5.0) + 1);

    return res;
}

// ----------------------------------------------------------------------------------------------------------------------------
// class TachoGenerator

double TachoGenerator::generate(const std::chrono::microseconds& time)
{
    double_t seconds = static_cast<double_t>(time.count()) / 1000000.0;

    double_t res = 4000.0 * (sin(seconds / 15.0) + 1.0) + 1000.0 * (sin(seconds / 2.0) + 1);

    return res;
}
