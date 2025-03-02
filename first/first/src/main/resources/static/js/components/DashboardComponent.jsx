import React, { useState, useEffect } from 'react';
import { BarChart, Bar, XAxis, YAxis, CartesianGrid, Tooltip, Legend, ResponsiveContainer, PieChart, Pie, Cell } from 'recharts';

// Color palette for the charts
const COLORS = ['#0088FE', '#00C49F', '#FFBB28', '#FF8042', '#8884d8', '#82ca9d'];

// Status translations for better readability
const STATUS_TRANSLATIONS = {
    'PENDING': 'Pending Review',
    'APPROVED': 'Published',
    'REJECTED': 'Rejected',
    'REVISION_NEEDED': 'Needs Revision'
};

const DashboardComponent = () => {
    const [statistics, setStatistics] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState(null);
    const [totalArticles, setTotalArticles] = useState(0);

    useEffect(() => {
        const fetchStatistics = async () => {
            try {
                const response = await fetch('/articles/api/statistics');
                if (!response.ok) {
                    throw new Error('Failed to fetch statistics');
                }
                const data = await response.json();
                setStatistics(data);
                setTotalArticles(data.totalArticles);
            } catch (err) {
                setError(err.message);
            } finally {
                setLoading(false);
            }
        };

        fetchStatistics();

        // Set up auto-refresh every 5 minutes
        const interval = setInterval(fetchStatistics, 300000);
        return () => clearInterval(interval);
    }, []);

    if (loading) {
        return (
            <div className="flex items-center justify-center p-5">
                <div className="w-16 h-16 border-4 border-t-blue-500 border-blue-200 rounded-full animate-spin"></div>
            </div>
        );
    }

    if (error) {
        return (
            <div className="bg-red-50 border border-red-200 text-red-700 px-4 py-3 rounded relative">
                <strong className="font-bold">Error!</strong>
                <span className="block sm:inline"> {error}</span>
            </div>
        );
    }

    if (!statistics) {
        return null;
    }

    // Transform data for charts
    const domainData = Object.entries(statistics.articlesByDomain || {}).map(([domain, count]) => ({
        name: domain,
        count: count
    }));

    const statusData = Object.entries(statistics.articlesByStatus || {}).map(([status, count]) => ({
        name: STATUS_TRANSLATIONS[status] || status,
        value: count
    }));

    // Calculate percentages for the status pie chart
    const totalStatusArticles = statusData.reduce((sum, item) => sum + item.value, 0);
    const statusDataWithPercentage = statusData.map(item => ({
        ...item,
        percentage: ((item.value / totalStatusArticles) * 100).toFixed(1)
    }));

    return (
        <div className="space-y-6">
            {/* Summary Cards */}
            <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
                <div className="bg-blue-500 rounded-lg shadow-lg p-6 text-white">
                    <h3 className="text-lg font-semibold mb-2">Total Articles</h3>
                    <div className="text-3xl font-bold">{totalArticles}</div>
                </div>

                <div className="bg-green-500 rounded-lg shadow-lg p-6 text-white">
                    <h3 className="text-lg font-semibold mb-2">Published Articles</h3>
                    <div className="text-3xl font-bold">
                        {statistics.articlesByStatus?.APPROVED || 0}
                    </div>
                </div>

                <div className="bg-yellow-500 rounded-lg shadow-lg p-6 text-white">
                    <h3 className="text-lg font-semibold mb-2">Pending Review</h3>
                    <div className="text-3xl font-bold">
                        {statistics.articlesByStatus?.PENDING || 0}
                    </div>
                </div>
            </div>

            {/* Charts */}
            <div className="grid grid-cols-1 lg:grid-cols-2 gap-6">
                {/* Domain Distribution */}
                <div className="bg-white rounded-lg shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Articles by Domain</h3>
                    <div className="h-96">
                        <ResponsiveContainer width="100%" height="100%">
                            <BarChart data={domainData}>
                                <CartesianGrid strokeDasharray="3 3" />
                                <XAxis dataKey="name" />
                                <YAxis />
                                <Tooltip />
                                <Legend />
                                <Bar
                                    dataKey="count"
                                    fill="#3B82F6"
                                    name="Number of Articles"
                                    radius={[4, 4, 0, 0]}
                                />
                            </BarChart>
                        </ResponsiveContainer>
                    </div>
                </div>

                {/* Status Distribution */}
                <div className="bg-white rounded-lg shadow-lg p-6">
                    <h3 className="text-lg font-semibold mb-4">Articles by Status</h3>
                    <div className="h-96">
                        <ResponsiveContainer width="100%" height="100%">
                            <PieChart>
                                <Pie
                                    data={statusDataWithPercentage}
                                    cx="50%"
                                    cy="50%"
                                    labelLine={false}
                                    label={({ name, percentage }) => `${name}: ${percentage}%`}
                                    outerRadius={80}
                                    fill="#8884d8"
                                    dataKey="value"
                                >
                                    {statusDataWithPercentage.map((entry, index) => (
                                        <Cell
                                            key={`cell-${index}`}
                                            fill={COLORS[index % COLORS.length]}
                                        />
                                    ))}
                                </Pie>
                                <Tooltip
                                    formatter={(value, name) => [
                                        `${value} articles (${((value / totalStatusArticles) * 100).toFixed(1)}%)`,
                                        name
                                    ]}
                                />
                            </PieChart>
                        </ResponsiveContainer>
                    </div>
                </div>
            </div>

            {/* Legend for Status Colors */}
            <div className="bg-white rounded-lg shadow-lg p-6">
                <h3 className="text-lg font-semibold mb-4">Status Legend</h3>
                <div className="grid grid-cols-2 md:grid-cols-4 gap-4">
                    {statusDataWithPercentage.map((status, index) => (
                        <div key={status.name} className="flex items-center space-x-2">
                            <div
                                className="w-4 h-4 rounded-full"
                                style={{ backgroundColor: COLORS[index % COLORS.length] }}
                            />
                            <span>{status.name}</span>
                        </div>
                    ))}
                </div>
            </div>
        </div>
    );
};

export default DashboardComponent;